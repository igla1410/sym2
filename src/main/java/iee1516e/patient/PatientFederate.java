package iee1516e.patient;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAboolean;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.encoding.HLAinteger64BE;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import iee1516e.office.Office;
import iee1516e.office.OfficeFederate;
import iee1516e.utils.StdRandom;

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
 * Created by piotr on 14.09.2015.
 */
public class PatientFederate
{
    private static final int MIN_PATIENT_TIME = 30;
    private static final int MAX_PATIENT_TIME = 100;
    private static final double CITO_PROB = 0.05;

    public static final String READY_TO_RUN = "ReadyToRun";
    public static RTIambassador rtiamb;
    private PatientFederateAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory;
    protected EncoderFactory encoderFactory;
    protected Map<ObjectInstanceHandle, Patient> patients = new LinkedHashMap<>();
    protected List<PatientEvent> inGabinetEvents = new ArrayList<>();
    protected List<PatientEvent> patientWaitingEvents = new ArrayList<>();

    public void runFederate(String federateName) throws Exception
    {
        if (prepareFederate(federateName)) return;
        publishAndSubscribe();
        log("Published and Subscribed");
        while (fedamb.running)
        {
            /*
            handlers
             */
            ObjectInstanceHandle objectHandle = registerObject();
            Patient patient = updateAttributeValues(objectHandle);
            patients.put(objectHandle, patient);
            log("Przyszed� pacjent do lekarza " + patient.getDoctorPreference() + " id= " + objectHandle);
            advanceTime(StdRandom.uniform(MIN_PATIENT_TIME, MAX_PATIENT_TIME));

        }
        log("Koniec pracy!");
        destroyFederate(patients);
    }

    private void handleEvents()
    {

    }

    /*private void handleInRegistrationEvents() throws RTIexception
    {
        for( PatientEvent patientEvent: inRegistrationEvents)
        {
            ObjectInstanceHandle objectInstanceHandle=getObjectInstanceHandleFromPatientId(patientEvent.getIdPatient());
            Patient patient=patients.get(objectInstanceHandle);

            ObjectClassHandle patientHandle=rtiamb.getObjectClassHandle("HLAobjectRoot.Patient");
            AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(1);

            inRegistrationEvents.remove(patientEvent);
        }
    }*/

    private void handlePatientWaitingRegistrationEvents() throws RTIexception
    {
        for (PatientEvent patientEvent : patientWaitingEvents)
        {
            ObjectInstanceHandle objectInstanceHandle = getObjectInstanceHandleFromPatientId(patientEvent.getIdPatient());
            Patient patient = patients.get(objectInstanceHandle);

            ObjectClassHandle patientHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Patient");
            AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(3);
            AttributeHandle registrationTimeHandle = rtiamb.getAttributeHandle(patientHandle, "registrationTime");
            long registrationTime = patientEvent.getTime() - patient.getRegistrationTime();
            HLAinteger64BE registrationTimeVal = encoderFactory.createHLAinteger64BE(registrationTime);
            attributes.put(registrationTimeHandle, registrationTimeVal.toByteArray());

            AttributeHandle registered = rtiamb.getAttributeHandle(patientHandle, "registered");
            HLAboolean registeredVal = encoderFactory.createHLAboolean(true);
            attributes.put(registered, registeredVal.toByteArray());

            AttributeHandle waitingTime = rtiamb.getAttributeHandle(patientHandle, "waitingTime");
            HLAinteger64BE waitingTimeVal = encoderFactory.createHLAinteger64BE(getTimeAsShort());
            attributes.put(waitingTime, waitingTimeVal.toByteArray());

            patientWaitingEvents.remove(patientEvent);
            HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
            rtiamb.updateAttributeValues(objectInstanceHandle, attributes, generateTag(), time);
        }
    }

    private void handleInGabinetEvents() throws RTIexception
    {
        for (PatientEvent patientEvent : inGabinetEvents)
        {
            ObjectInstanceHandle objectInstanceHandle = getObjectInstanceHandleFromPatientId(patientEvent.getIdPatient());
            Patient patient = patients.get(objectInstanceHandle);
            ObjectClassHandle patientHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Patient");
            AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(3);
            AttributeHandle waitingTimeHandler = rtiamb.getAttributeHandle(patientHandle, "waitingTime");
            long waitingTime = patientEvent.getTime() - patient.getWaitingTime();
            HLAinteger64BE waitingTimeVal = encoderFactory.createHLAinteger64BE(waitingTime);
            attributes.put(waitingTimeHandler, waitingTimeVal.toByteArray());


            inGabinetEvents.remove(patientEvent);
            HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
            rtiamb.updateAttributeValues(objectInstanceHandle, attributes, generateTag(), time);
        }
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

    private Patient updateAttributeValues(ObjectInstanceHandle objectHandle) throws RTIexception
    {
        ObjectClassHandle patientHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Patient");
        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(3);
        Patient patient = new Patient();
        AttributeHandle registrationTimeHandle = rtiamb.getAttributeHandle(patientHandle, "registrationTime");
        AttributeHandle citoHandle = rtiamb.getAttributeHandle(patientHandle, "cito");
        AttributeHandle preferenceHandle = rtiamb.getAttributeHandle(patientHandle, "doctorPreference");
        //set cito
        boolean isCitoValue = StdRandom.bernoulli(CITO_PROB);
        HLAboolean isCito = encoderFactory.createHLAboolean(isCitoValue);
        attributes.put(citoHandle, isCito.toByteArray());
        patient.setCito(isCitoValue);
        //set doctor preference: -1 for patient without preference
        int doctorPreferenceValue = StdRandom.uniform(-1, OfficeFederate.COUNT);
        patient.setDoctorPreference(doctorPreferenceValue);
        HLAinteger32BE doctorPreference = encoderFactory.createHLAinteger32BE(doctorPreferenceValue);
        attributes.put(preferenceHandle, doctorPreference.toByteArray());
        HLAinteger64BE registrationTime = encoderFactory.createHLAinteger64BE(getTimeAsShort());
        attributes.put(registrationTimeHandle, registrationTime.toByteArray());
        patient.setRegistrationTime(getTimeAsShort());
        patient.setIdPatient(Integer.valueOf(patientHandle.toString()));

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.updateAttributeValues(objectHandle, attributes, generateTag(), time);
        return patient;
    }

    private ObjectInstanceHandle registerObject() throws RTIexception
    {
        ObjectClassHandle patientHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Patient");
        return rtiamb.registerObjectInstance(patientHandle);
    }

    public ObjectInstanceHandle getObjectInstanceHandleFromPatientId(int id)
    {
        for (Map.Entry<ObjectInstanceHandle, Patient> entry : patients.entrySet())
        {
            if (entry.getValue().getIdPatient() == id)
            {
                return entry.getKey();
            }
        }

        return null;
    }

    private void publishAndSubscribe() throws RTIexception
    {
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
        rtiamb.publishObjectClassAttributes(patientHandle, attributes);

        InteractionClassHandle workStatusSubHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.workStatus");
        rtiamb.subscribeInteractionClass(workStatusSubHandle);
        InteractionClassHandle inGabinetHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.inGabinet");
        rtiamb.subscribeInteractionClass(inGabinetHandle);
        InteractionClassHandle outGabinetHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.outGabinet");
        rtiamb.subscribeInteractionClass(outGabinetHandle);
        InteractionClassHandle inWaitingRoomHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.patientWaiting");
        rtiamb.subscribeInteractionClass(inWaitingRoomHandle);
        InteractionClassHandle inRegistrationHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.inRegistration");
        rtiamb.subscribeInteractionClass(inRegistrationHandle);
    }

    private void destroyFederate(Map<ObjectInstanceHandle, Patient> patients) throws RTIexception
    {
        for (ObjectInstanceHandle oh : patients.keySet())
        {
            deleteObject(oh);
            log("Deleted Object, handle=" + oh);
        }
        patients.clear();

        rtiamb.resignFederationExecution(ResignAction.DELETE_OBJECTS);
        log("Resigned from Federation");

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
    }

    private boolean prepareFederate(String federateName) throws Exception
    {
        log("Creating RTIambassador");
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

        log("Connecting...");
        fedamb = new PatientFederateAmbassador(this);
        rtiamb.connect(fedamb, CallbackModel.HLA_EVOKED);

        log("Creating Federation...");
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
                "PatientFederateType",   // federate type
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

    private void enableTimePolicy() throws Exception
    {
        HLAfloat64Interval lookahead = timeFactory.makeInterval(fedamb.federateLookahead);
        this.rtiamb.enableTimeRegulation(lookahead);
        while (!fedamb.isRegulating)
        {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
        this.rtiamb.enableTimeConstrained();

        while (!fedamb.isConstrained)
        {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private void deleteObject(ObjectInstanceHandle handle) throws RTIexception
    {
        rtiamb.deleteObjectInstance(handle, generateTag());
    }

    public short getTimeAsShort()
    {
        return (short) (fedamb != null ? fedamb.federateTime : 0);
    }

    byte[] generateTag()
    {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    public static void main(String[] args)
    {

        String federateName = "PatientFederate";
        if (args.length != 0)
        {
            federateName = args[0];
        }
        try
        {
            new PatientFederate().runFederate(federateName);
        }
        catch (Exception rtie)
        {
            rtie.printStackTrace();
        }
    }

    private void log(String message)
    {
        System.out.println("czas: " + getTimeAsShort() + " - PatientFederate   : " + message);
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
}
