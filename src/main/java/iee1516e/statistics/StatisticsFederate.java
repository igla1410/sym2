package iee1516e.statistics;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import iee1516e.patient.Patient;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by piotr on 16.09.2015.
 */
public class StatisticsFederate
{
    public static final String READY_TO_RUN = "ReadyToRun";
    public static RTIambassador rtiamb;
    private StatisticsFederateAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory;
    protected EncoderFactory encoderFactory;
    Map<ObjectInstanceHandle, Patient> patients = new LinkedHashMap<>();

    public void runFederate(String federateName) throws Exception
    {
        if (prepareFederate(federateName)) return;

        publishAndSubscribe();
        log("Published and Subscribed");
        List<ObjectInstanceHandle> patientsL = new ArrayList<>();

        while (fedamb.running)
        {

            advanceTime(1.0);

        }
        log("Przychodnia konczy prace");
        log("Liczba wszystkich pacjentów: " + patients.size());
        log("Liczba obsluzonych pacjentów: " + patientServed());
        log("Procent utraconych klientow: " + percentLost() + "%");
        log("Sredni czas oczekiwania na stolik: " + obliczSredniCzasWKolejce());
        log("Sredni czas oczekiwania na posilek: " + obliczSredniCzasOczekiwania());
        destroyFederate(patientsL);
    }
    private long patientServed()
    {
        return patients.values().stream().filter(p -> p.isRegistered()==Boolean.TRUE).count();
    }
    private Double percentLost()
    {
        return roundToTwoDecimalPlaces((Double.valueOf(patients.size()-patientServed())/patients.size())*100.D);
    }
    private double averageWaitingTime()
    {

    }
    private void destroyFederate(List<ObjectInstanceHandle> clients) throws RTIexception
    {
        //Thread.sleep(2000);
        for (ObjectInstanceHandle objectHandle : clients) {
            deleteObject(objectHandle);
            log("Deleted Object, handle=" + objectHandle);
        }

        try {
            rtiamb.resignFederationExecution(ResignAction.DELETE_OBJECTS);
            log("Resigned from Federation");
        }catch (Exception ex){
            log("Timeout");
        }

        try {
            rtiamb.destroyFederationExecution("ExampleFederation");
            log("Destroyed Federation");
        } catch (FederationExecutionDoesNotExist dne) {
            log("No need to destroy federation, it doesn't exist");
        } catch (FederatesCurrentlyJoined fcj) {
            log("Didn't destroy federation, federates still joined");
        } catch (Exception ex){
            log("Some other error");
        }
    }

    private boolean prepareFederate(String federateName) throws Exception {
        log("Creating RTIambassador");
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

        log("Connecting...");
        fedamb = new StatisticsFederateAmbassador(this);
        rtiamb.connect(fedamb, CallbackModel.HLA_EVOKED);

        log("Creating Federation...");
        try {
            URL[] modules = new URL[]{
                    (new File("foms/client.xml")).toURI().toURL()
            };

            rtiamb.createFederationExecution("ExampleFederation", modules);
            log("Created Federation");
        } catch (FederationExecutionAlreadyExists exists) {
            log("Didn't create federation, it already existed");
        } catch (MalformedURLException urle) {
            log("Exception loading one of the FOM modules from disk: " + urle.getMessage());
            urle.printStackTrace();
            return true;
        }

        URL[] joinModules = new URL[]{
        };

        rtiamb.joinFederationExecution(federateName,            // name for the federate
                "StatystykaFederateType",   // federate type
                "ExampleFederation",     // name of federation
                joinModules);           // modules we want to add

        log("Joined Federation as " + federateName);

        this.timeFactory = (HLAfloat64TimeFactory) rtiamb.getTimeFactory();


        rtiamb.registerFederationSynchronizationPoint(READY_TO_RUN, null);
        // wait until the point is announced
        while (!fedamb.isAnnounced) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        waitForUser();

        rtiamb.synchronizationPointAchieved(READY_TO_RUN);
        log("Achieved sync point: " + READY_TO_RUN + ", waiting for federation...");
        while (!fedamb.isReadyToRun) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        enableTimePolicy();
        log("Time Policy Enabled");
        return false;
    }

    private void enableTimePolicy() throws Exception {
        HLAfloat64Interval lookahead = timeFactory.makeInterval(fedamb.federateLookahead);
        rtiamb.enableTimeRegulation(lookahead);
        while (!fedamb.isRegulating) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
        rtiamb.enableTimeConstrained();

        while (!fedamb.isConstrained) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private void publishAndSubscribe() throws RTIexception {
        ObjectClassHandle patientHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Patient");
        AttributeHandle idHandle = rtiamb.getAttributeHandle(patientHandle, "idPatient");
        AttributeHandle citoHandle = rtiamb.getAttributeHandle(patientHandle, "cito");
        AttributeHandle preferenceHandle = rtiamb.getAttributeHandle(patientHandle, "doctorPreference");
        AttributeHandle registrationTimeHandle = rtiamb.getAttributeHandle(patientHandle, "registrationTime");
        AttributeHandle waitingTimeHandle = rtiamb.getAttributeHandle(patientHandle, "waitingTime");
        AttributeHandle registeredHandle = rtiamb.getAttributeHandle(patientHandle, "registered");
        AttributeHandleSet attributes = rtiamb.getAttributeHandleSetFactory().create();
        attributes.add(idHandle);
        attributes.add(citoHandle);
        attributes.add(preferenceHandle);
        attributes.add(registrationTimeHandle);
        attributes.add(waitingTimeHandle);
        attributes.add(registeredHandle);
        rtiamb.subscribeObjectClassAttributes(patientHandle, attributes);


    }

    private void advanceTime(double timestep) throws RTIexception {

        fedamb.isAdvancing = true;
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + timestep);
        rtiamb.timeAdvanceRequest(time);

        while (fedamb.isAdvancing) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    void deleteObject(ObjectInstanceHandle handle) throws RTIexception {
        rtiamb.deleteObjectInstance(handle, generateTag());
    }

    public short getTimeAsShort() {
        return (short) (fedamb != null ? fedamb.federateTime : 0);
    }
    private double roundToTwoDecimalPlaces(double number){
        number = Math.round(number * 100);
        return number/100;
    }

    byte[] generateTag() {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    public static void main(String[] args) {

        String federateName = "StatisticsFederate";
        if (args.length != 0) {
            federateName = args[0];
        }

        try {
            new StatisticsFederate().runFederate(federateName);
        } catch (Exception rtie) {
            rtie.printStackTrace();
        }
    }

    private void log(String message) {
        System.out.println("czas: " + getTimeAsShort() + " - StatisticsFederate   : " + message);
    }

    private void waitForUser() {
        log(" >>>>>>>>>> Press Enter to Continue <<<<<<<<<<");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            reader.readLine();
        } catch (Exception e) {
            log("Error while waiting for user input: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
