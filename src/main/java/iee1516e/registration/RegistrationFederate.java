package iee1516e.registration;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import iee1516e.office.Office;
import iee1516e.patient.Patient;
import iee1516e.utils.StdRandom;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by piotr on 14.09.2015.
 */
public class RegistrationFederate
{
    public static final String READY_TO_RUN = "ReadyToRun";
    public static RTIambassador rtiamb;
    private RegistrationFederateAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory;
    protected EncoderFactory encoderFactory;
    private static final double MIN_REGISTRATION_TIME = 0.5;
    private static final double MAX_REGISTRATION_TIME = 2.0;
    Map<ObjectInstanceHandle, Patient> patientsToRegister = new LinkedHashMap<>();
    Map<Integer, Integer> registrationLimits = new LinkedHashMap<>();
    Map<Integer, Integer> patientsRegistered = new LinkedHashMap<>();

    public void runFederate(String federateName) throws Exception
    {
        if (prepareFederate(federateName)) return;
        publishAndSubscribe();
        log("Published and Subscribed");
        while (fedamb.running)
        {
            if (!registrationLimits.isEmpty())
                tryRegister();
            advanceTime(StdRandom.uniform(MIN_REGISTRATION_TIME, MAX_REGISTRATION_TIME));
        }
        log("Przychodnia koñczy pracê");
        destroyFederate(patientsToRegister);
    }

    private void sendWorkStatusInteraction(int typKomunikatu) throws RTIexception
    {
        InteractionClassHandle zamkniecieRestauracjiHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.workStatus");
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(1);

        ParameterHandle typKomunikatuHandle = rtiamb.getParameterHandle(zamkniecieRestauracjiHandle, "typKomunikatu");
        HLAinteger32BE typKomunikatuVal = encoderFactory.createHLAinteger32BE(typKomunikatu);
        parameters.put(typKomunikatuHandle, typKomunikatuVal.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.sendInteraction(zamkniecieRestauracjiHandle, parameters, generateTag(), time);
    }

    private void publishAndSubscribe() throws RTIexception
    {
        //publish inRegistration
        InteractionClassHandle inRegistrationHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.inRegistration");
        rtiamb.publishInteractionClass(inRegistrationHandle);

        //subsribe Patient
        ObjectClassHandle patientHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Patient");
        AttributeHandle idPatientHandle = rtiamb.getAttributeHandle(patientHandle, "idPatient");
        AttributeHandle citoHandle = rtiamb.getAttributeHandle(patientHandle, "cito");
        AttributeHandle preferenceHandle = rtiamb.getAttributeHandle(patientHandle, "doctorPreference");
        AttributeHandle registrationTimeHandle = rtiamb.getAttributeHandle(patientHandle, "registrationTime");
        AttributeHandle visitTimeHandle = rtiamb.getAttributeHandle(patientHandle, "visitTime");
        AttributeHandle waitingTimeHandle = rtiamb.getAttributeHandle(patientHandle, "waitingTime");
        AttributeHandle registeredHandle=rtiamb.getAttributeHandle(patientHandle,"registered");
        AttributeHandleSet attributes = rtiamb.getAttributeHandleSetFactory().create();
        attributes.add(idPatientHandle);
        attributes.add(citoHandle);
        attributes.add(preferenceHandle);
        attributes.add(registrationTimeHandle);
        attributes.add(visitTimeHandle);
        attributes.add(waitingTimeHandle);
        attributes.add(registeredHandle);
        rtiamb.subscribeObjectClassAttributes(patientHandle, attributes);


        InteractionClassHandle workStatusHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.workStatus");
        rtiamb.publishInteractionClass(workStatusHandle);
        InteractionClassHandle workStatusSubHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.workStatus");
        rtiamb.subscribeInteractionClass(workStatusSubHandle);
        //subscribe Gabinet
    /*    ObjectClassHandle gabinetHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Gabinet");
        AttributeHandle idHandle = rtiamb.getAttributeHandle(gabinetHandle, "idGabinet");
        AttributeHandle idPatientGabinetHandle = rtiamb.getAttributeHandle(gabinetHandle, "idPatient");
        AttributeHandle stateHandle = rtiamb.getAttributeHandle(gabinetHandle, "state");
        AttributeHandle registrationLimitHandle = rtiamb.getAttributeHandle(gabinetHandle, "registrationLimit");
        AttributeHandleSet gabinetAttributes = rtiamb.getAttributeHandleSetFactory().create();
        gabinetAttributes.add(idHandle);
        gabinetAttributes.add(idPatientGabinetHandle);
        gabinetAttributes.add(stateHandle);
        gabinetAttributes.add(registrationLimitHandle);
        rtiamb.subscribeObjectClassAttributes(gabinetHandle, gabinetAttributes);*/

        InteractionClassHandle gabinetOpenedHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.gabinetOpened");
        rtiamb.subscribeInteractionClass(gabinetOpenedHandle);
        InteractionClassHandle inWaitingRoomHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.patientWaiting");
        rtiamb.publishInteractionClass(inWaitingRoomHandle);

    }

    private void sendInRegistrationInteraction(Patient patient) throws NameNotFound, NotConnected, RTIinternalError, FederateNotExecutionMember, InvalidInteractionClassHandle, SaveInProgress, RestoreInProgress, InteractionClassNotPublished, InteractionClassNotDefined, InvalidLogicalTime, InteractionParameterNotDefined
    {
        InteractionClassHandle inRegistrationHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.inRegistration");
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);

        ParameterHandle patientIdHandle = rtiamb.getParameterHandle(inRegistrationHandle, "idPatient");
        HLAinteger32BE patientId = encoderFactory.createHLAinteger32BE(patient.getIdPatient());
        parameters.put(patientIdHandle, patientId.toByteArray());

        ParameterHandle doctorPreferenceHandle = rtiamb.getParameterHandle(inRegistrationHandle, "idGabinet");
        HLAinteger32BE doctorPreference = encoderFactory.createHLAinteger32BE(patient.getDoctorPreference());
        parameters.put(doctorPreferenceHandle, doctorPreference.toByteArray());
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.sendInteraction(inRegistrationHandle, parameters, generateTag(), time);

    }

    private void sendInWaitingRoomInteraction(int idPatient, int idGabinet) throws NameNotFound, NotConnected, RTIinternalError, FederateNotExecutionMember, InvalidInteractionClassHandle, SaveInProgress, RestoreInProgress, InteractionClassNotPublished, InteractionClassNotDefined, InvalidLogicalTime, InteractionParameterNotDefined
    {
        InteractionClassHandle inWaitingRoom = rtiamb.getInteractionClassHandle("HLAinteractionRoot.patientWaiting");
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);

        ParameterHandle idPatientHandle = rtiamb.getParameterHandle(inWaitingRoom, "idPatient");
        HLAinteger32BE patientId = encoderFactory.createHLAinteger32BE(idPatient);
        parameters.put(idPatientHandle, patientId.toByteArray());

        ParameterHandle idGabinetHandle = rtiamb.getParameterHandle(inWaitingRoom, "idGabinet");
        HLAinteger32BE idGabinetHLA = encoderFactory.createHLAinteger32BE(idGabinet);
        parameters.put(idGabinetHandle, idGabinetHLA.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.sendInteraction(inWaitingRoom, parameters, generateTag(), time);

    }

    private void tryRegister() throws RTIexception
    {
        Iterator<Map.Entry<ObjectInstanceHandle, Patient>> iterator = patientsToRegister.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry patient = iterator.next();
            Patient patientVal = (Patient) patient.getValue();
            ObjectInstanceHandle handle = (ObjectInstanceHandle) patient.getKey();
            patientVal.setIdPatient(Integer.valueOf(handle.toString()));
            sendInRegistrationInteraction(patientVal);
            int gabinet = findGabinet(patientVal);

            if (gabinet > -1)
            {
                log("Patient " + patientVal.getIdPatient() + " zarejestrowany do lekarza " + gabinet);
                sendInWaitingRoomInteraction(patientVal.getIdPatient(), gabinet);
                patientsToRegister.remove(patient.getKey());
                int queueLen = patientsRegistered.get(Integer.valueOf(gabinet));
                queueLen++;
                patientsRegistered.replace(Integer.valueOf(gabinet), queueLen);
            }
            else
            {
                sendWorkStatusInteraction(1);
                log("Koniec miejsc w rejestracji!");
                fedamb.running = false;
            }
        }
    }


    int getPatientsRegisteredById(int officeId)
    {
        return patientsRegistered.get(Integer.valueOf(officeId));
    }

    private int findGabinet(Patient patient)
    {
        if (patient.getDoctorPreference() != -1 && (patient.isCito() || patientsRegistered.size() == 0))
        {
            return patient.getDoctorPreference();
        }
        Integer min = Integer.MAX_VALUE;
        for (Map.Entry<Integer, Integer> office : registrationLimits.entrySet())
        {
            if (getPatientsRegisteredById(office.getKey()) < office.getValue())
            {
                if (office.getKey().equals(patient.getDoctorPreference()))
                {
                    return office.getKey();
                }
                if (getPatientsRegisteredById(office.getKey()) < min)
                {
                    min = getPatientsRegisteredById(office.getKey());
                }
            }
        }
        if (min == Integer.MAX_VALUE)
        {
            return -1;
        }
        return min;
    }

    private void advanceTime(double timestep) throws RTIexception
    {

        fedamb.isAdvancing = true;
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + timestep);
        rtiamb.timeAdvanceRequest(time);

        while (fedamb.isAdvancing)
        {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private void destroyFederate(Map<ObjectInstanceHandle, Patient> patients) throws RTIexception
    {
        //Thread.sleep(1000);
        Iterator<Map.Entry<ObjectInstanceHandle, Patient>> iterator = patientsToRegister.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry patient = iterator.next();
            Patient patientVal = (Patient) patient.getValue();
            ObjectInstanceHandle object = (ObjectInstanceHandle) patient.getKey();

            deleteObject(object);
            log("Deleted Object, handle=" + object);

        }
        patients.clear();
        try
        {
            rtiamb.resignFederationExecution(ResignAction.DELETE_OBJECTS);
            log("Resigned from Federation");
        }
        catch (Exception ex)
        {
            log("Timeout");
        }

        try
        {
            rtiamb.destroyFederationExecution("ExampleFederation");
            log("Destroyed Federation");
        }
        catch (FederationExecutionDoesNotExist dne)
        {
            log("No need to destroy federation, it doesn't exist");
        }
        catch (FederatesCurrentlyJoined fcj)
        {
            log("Didn't destroy federation, federates still joined");
        }
        catch (Exception ex)
        {
            log("Some other error");
        }
    }

    private boolean prepareFederate(String federateName) throws Exception
    {
        log("Creating RTIAmbassador");
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
        log("Connecting...");
        fedamb = new RegistrationFederateAmbassador(this);
        rtiamb.connect(fedamb, CallbackModel.HLA_EVOKED);
        log("Creating Federation");
        try
        {
            URL[] modules = new URL[]{
                    (new File("foms/client.xml")).toURI().toURL()
            };
            rtiamb.createFederationExecution("ExampleFederation", modules);
            log("Created Federation");
        }
        catch (FederationExecutionAlreadyExists exists)
        {
            log("Didn't create federation, it already existed");
        }
        catch (MalformedURLException urle)
        {
            log("Exception loading one of the FOM modules from disk: " + urle.getMessage());
            urle.printStackTrace();
            return true;
        }
        URL[] joinModules = new URL[]{
        };
        rtiamb.joinFederationExecution(federateName,            // name for the federate
                "RegistrationFederateType",   // federate type
                "ExampleFederation",     // name of federation
                joinModules);           // modules we want to add

        log("Joined Federation as " + federateName);
        this.timeFactory = (HLAfloat64TimeFactory) rtiamb.getTimeFactory();
        rtiamb.registerFederationSynchronizationPoint(READY_TO_RUN, null);
        // wait until the point is announced
        while (!fedamb.isAnnounced)
        {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        waitForUser();

        rtiamb.synchronizationPointAchieved(READY_TO_RUN);
        log("Achieved sync point: " + READY_TO_RUN + ", waiting for federation...");
        while (!fedamb.isReadyToRun)
        {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        enableTimePolicy();
        log("Time Policy Enabled");
        return false;
    }

    private void deleteObject(ObjectInstanceHandle handle) throws RTIexception
    {
        rtiamb.deleteObjectInstance(handle, generateTag());
    }

    private void enableTimePolicy() throws Exception
    {
        HLAfloat64Interval lookahead = timeFactory.makeInterval(fedamb.federateLookahead);
        rtiamb.enableTimeRegulation(lookahead);
        while (!fedamb.isRegulating)
        {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
        rtiamb.enableTimeConstrained();

        while (!fedamb.isConstrained)
        {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private void log(String message)
    {
        System.out.println("czas: " + getTimeAsShort() + " - RegistrationFederate   : " + message);
    }

    public short getTimeAsShort()
    {
        return (short) (fedamb != null ? fedamb.federateTime : 0);
    }

    private byte[] generateTag()
    {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    private void waitForUser()
    {
        log(" >>>>>>>>>> Press Enter to Continue <<<<<<<<<<");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try
        {
            reader.readLine();
        }
        catch (Exception e)
        {
            log("Error while waiting for user input: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        String federateName = "RegistrationFederate";
        if (args.length != 0)
            federateName = args[0];
        try
        {
            new RegistrationFederate().runFederate(federateName);
        }
        catch (Exception rtie)
        {
            rtie.printStackTrace();
        }

    }
}
