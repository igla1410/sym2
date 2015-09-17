package iee1516e.office;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import iee1516e.utils.StdRandom;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by piotr on 15.09.2015.
 */
public class OfficeFederate
{
    public static final int COUNT = 5;
    private static final Integer SIMULATION_TIME = 500
            ;
    private static final int MIN_REG = 30;
    private static final int MAX_REG = 100;
    private static final int MIN_VIS = 50;
    private static final int MAX_VIS = 200;
    private static final double BREAK_PROB = 0.1;
    public static final String READY_TO_RUN = "ReadyToRun";
    public static RTIambassador rtiamb;
    private OfficeFederateAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory;
    protected EncoderFactory encoderFactory;
    Map<Integer, Office> offices = new LinkedHashMap<>();
    ArrayList<PatientGabinet> patientsToAnnouce = new ArrayList<>();
    private boolean send = false;

    public void runFederate(String federateName) throws Exception
    {
        if (prepareFederate(federateName)) return;
        publishAndSubscribe();
        generateOffices();
        log("Published and Subscribed");
        List<ObjectInstanceHandle> gabinets = new ArrayList<>();
       /* for (int i=0;i<COUNT;i++)
        {
            ObjectInstanceHandle objectHandle=registerObject();
            gabinets.add(objectHandle);
            updateAttributeValues(objectHandle);
            log("Utworzono gabinet: o id="+objectHandle);
        }*/

        while (getTimeAsShort() <= SIMULATION_TIME || !allGabinetsBusy())
        {
            if (getTimeAsShort() <= SIMULATION_TIME)
            {

            }
            else if (!send)
            {
                sendWorkStatusInteraction(0);
                send = true;
            }
            handleAnnouncements();
            servePatients();
            advanceTime(StdRandom.uniform(MIN_VIS, MAX_VIS));
        }
        sendWorkStatusInteraction(-1);
        destroyFederate();
    }

    private void generateOffices() throws Exception
    {
        for (int i = 0; i < COUNT; i++)
        {
            Office office = new Office(i, -1, Office.STATE.FREE, StdRandom.uniform(MIN_REG, MAX_REG));
            sendGabinetOpenedInteraction(new GabinetOpenedEvent(office.getOfficeId(), office.getRegistrationLimit()));
            log("Utworzono gabinet: o id=" + office.getOfficeId() + "i limicie przyjêæ= " + office.getRegistrationLimit());
            offices.put(office.getOfficeId(), office);
        }
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

    private boolean allGabinetsBusy()
    {
        int decision = 1;
        Iterator<Map.Entry<Integer, Office>> iter = offices.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry entry = iter.next();
            Office office = (Office) entry.getValue();
            Office.STATE state = office.getState();
            int status = 1;
            switch (state)
            {
                case FREE:
                    status = 0;
                    break;
                case BUSY:
                    status = 1;
                    break;

                case BREAK:
                    status = 2;
                    break;

            }
            decision *= status;
        }
        return (decision == 1 ? true : false);
    }

    private void servePatients() throws RTIexception
    {
        Iterator<Map.Entry<Integer, Office>> iter = offices.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry entry = iter.next();
            Office office = (Office) entry.getValue();

            if (office.isBusy())
            {
                double roll = StdRandom.uniform();
                if (roll <= BREAK_PROB && !send)
                    office.setState(Office.STATE.BREAK);
                else
                    office.setState(Office.STATE.FREE);
                sendGabinetStatusChangedInteraction(office.getOfficeId(), office.getState());
                sendPatientOutGabinetInteraction(office.getIdPatient(), office.getOfficeId());
                log("Status "+office.getOfficeId()+" zmieniono na "+office.getState());
            }
        }
    }

    private void handleAnnouncements() throws RTIexception
    {
        for (PatientGabinet patientGabinet : patientsToAnnouce)
        {
            sendPatientInGabinetInteraction(patientGabinet.getIdPatient(), patientGabinet.getIdGabinet());
        }
    }

    private void sendGabinetStatusChangedInteraction(int idGabinet, Office.STATE state) throws RTIexception
    {
        InteractionClassHandle patientReadyHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.gabinetStatusChanged");
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);
        ParameterHandle idPatientHandle = rtiamb.getParameterHandle(patientReadyHandle, "idGabinet");
        int status = 0;
        switch (state)
        {
            case FREE:
                status = 0;
                break;
            case BUSY:
                status = 1;
                break;

            case BREAK:
                status = 2;
                break;

        }
        HLAinteger32BE patientId = encoderFactory.createHLAinteger32BE(status);
        parameters.put(idPatientHandle, patientId.toByteArray());

        ParameterHandle idGabinetHandle = rtiamb.getParameterHandle(patientReadyHandle, "idGabinet");
        HLAinteger32BE idGabinetHLA = encoderFactory.createHLAinteger32BE(idGabinet);
        parameters.put(idGabinetHandle, idGabinetHLA.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.sendInteraction(patientReadyHandle, parameters, generateTag(), time);
    }

    private void sendGabinetOpenedInteraction(GabinetOpenedEvent event) throws NameNotFound, FederateNotExecutionMember, NotConnected, RTIinternalError, InvalidInteractionClassHandle, InvalidLogicalTime, InteractionClassNotPublished, InteractionParameterNotDefined, InteractionClassNotDefined, SaveInProgress, RestoreInProgress
    {
        InteractionClassHandle gabinetOpenedEventHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.gabinetOpened");
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);

        ParameterHandle idGabinetHandle = rtiamb.getParameterHandle(gabinetOpenedEventHandle, "idGabinet");
        HLAinteger32BE idGabinet = encoderFactory.createHLAinteger32BE(event.getIdGabinet());
        parameters.put(idGabinetHandle, idGabinet.toByteArray());

        ParameterHandle registrationLimitHandle = rtiamb.getParameterHandle(gabinetOpenedEventHandle, "registrationLimit");
        HLAinteger32BE registrationLimit = encoderFactory.createHLAinteger32BE(event.getRegistrationLimit());
        parameters.put(registrationLimitHandle, registrationLimit.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.sendInteraction(gabinetOpenedEventHandle, parameters, generateTag(), time);

    }

    private void sendPatientInGabinetInteraction(int idPatient, int idGabinet) throws RTIexception
    {
        InteractionClassHandle patientReadyHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.inGabinet");
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);
        ParameterHandle idPatientHandle = rtiamb.getParameterHandle(patientReadyHandle, "idPatient");
        HLAinteger32BE patientId = encoderFactory.createHLAinteger32BE(idPatient);
        parameters.put(idPatientHandle, patientId.toByteArray());

        ParameterHandle idGabinetHandle = rtiamb.getParameterHandle(patientReadyHandle, "idGabinet");
        HLAinteger32BE idGabinetHLA = encoderFactory.createHLAinteger32BE(idGabinet);
        parameters.put(idGabinetHandle, idGabinetHLA.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.sendInteraction(patientReadyHandle, parameters, generateTag(), time);
    }

    private void sendPatientOutGabinetInteraction(int idPatient, int idGabinet) throws RTIexception
    {
        InteractionClassHandle patientReadyHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.outGabinet");
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);
        ParameterHandle idPatientHandle = rtiamb.getParameterHandle(patientReadyHandle, "idPatient");
        HLAinteger32BE patientId = encoderFactory.createHLAinteger32BE(idPatient);
        parameters.put(idPatientHandle, patientId.toByteArray());

        ParameterHandle idGabinetHandle = rtiamb.getParameterHandle(patientReadyHandle, "idGabinet");
        HLAinteger32BE idGabinetHLA = encoderFactory.createHLAinteger32BE(idGabinet);
        parameters.put(idGabinetHandle, idGabinetHLA.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.sendInteraction(patientReadyHandle, parameters, generateTag(), time);
    }

    private boolean prepareFederate(String federateName) throws Exception
    {
        log("Creating RTIambassador");
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

        log("Connecting...");
        fedamb = new OfficeFederateAmbassador(this);
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
                "OfficeFederateType",   // federate type
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

    private void publishAndSubscribe() throws RTIexception
    {
        //publish Gabinet
       /* ObjectClassHandle gabinetHandle=rtiamb.getObjectClassHandle("HLAobjectRoot.Gabinet");
        AttributeHandle idHandle=rtiamb.getAttributeHandle(gabinetHandle, "idGabinet");
        AttributeHandle idPatientHandle=rtiamb.getAttributeHandle(gabinetHandle,"idPatient");
        AttributeHandle stateHandle=rtiamb.getAttributeHandle(gabinetHandle,"state");
        AttributeHandle registrationLimitHandle=rtiamb.getAttributeHandle(gabinetHandle,"registrationLimit");
        AttributeHandleSet attributes=rtiamb.getAttributeHandleSetFactory().create();
        attributes.add(idHandle);
        attributes.add(idPatientHandle);
        attributes.add(stateHandle);
        attributes.add(registrationLimitHandle);
        rtiamb.publishObjectClassAttributes(gabinetHandle,attributes);*/

        InteractionClassHandle gabinetOpenedHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.gabinetOpened");
        rtiamb.publishInteractionClass(gabinetOpenedHandle);
        InteractionClassHandle patientReadyRoomHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.patientReady");
        rtiamb.subscribeInteractionClass(patientReadyRoomHandle);
        InteractionClassHandle gabinetStatusChangedHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.gabinetStatusChanged");
        rtiamb.publishInteractionClass(gabinetStatusChangedHandle);
        InteractionClassHandle inGabinetHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.inGabinet");
        rtiamb.publishInteractionClass(inGabinetHandle);
        InteractionClassHandle outGabinetHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.outGabinet");
        rtiamb.publishInteractionClass(outGabinetHandle);
        InteractionClassHandle workStatusHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.workStatus");
        rtiamb.publishInteractionClass(workStatusHandle);
    }

    private ObjectInstanceHandle registerObject() throws RTIexception
    {
        ObjectClassHandle gabinetHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Gabinet");
        return rtiamb.registerObjectInstance(gabinetHandle);
    }

    private void updateAttributeValues(ObjectInstanceHandle objectHandle) throws RTIexception
    {
        ObjectClassHandle gabinetHandle = rtiamb.getObjectClassHandle("HLAobjectRoot.Gabinet");

        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(2);

        AttributeHandle idPatientHandle = rtiamb.getAttributeHandle(gabinetHandle, "idPatient");
        HLAinteger32BE idPatient = encoderFactory.createHLAinteger32BE(-1);
        attributes.put(idPatientHandle, idPatient.toByteArray());

        AttributeHandle stateHandle = rtiamb.getAttributeHandle(gabinetHandle, "state");
        HLAinteger32BE state = encoderFactory.createHLAinteger32BE(0);
        attributes.put(stateHandle, state.toByteArray());

        AttributeHandle registrationLimitHandle = rtiamb.getAttributeHandle(gabinetHandle, "registrationLimit");
        HLAinteger32BE registrationLimit = encoderFactory.createHLAinteger32BE(StdRandom.uniform(MIN_REG, MAX_REG));
        attributes.put(stateHandle, state.toByteArray());
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.updateAttributeValues(objectHandle, attributes, generateTag(), time);
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

    private void destroyFederate() throws RTIexception
    {
        //Thread.sleep(1000);

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

    private void deleteObject(ObjectInstanceHandle handle) throws RTIexception
    {
        rtiamb.deleteObjectInstance(handle, generateTag());
    }

    public short getTimeAsShort()
    {
        return (short) (fedamb != null ? fedamb.federateTime : 0);
    }

    private byte[] generateTag()
    {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    private void log(String message)
    {
        System.out.println("czas: " + getTimeAsShort() + " - OfficeFederate   : " + message);
    }

    public static void main(String[] args)
    {

        String federateName = "OfficeFederate";
        if (args.length != 0)
        {
            federateName = args[0];
        }
        try
        {
            new OfficeFederate().runFederate(federateName);
        }
        catch (Exception rtie)
        {
            rtie.printStackTrace();
        }
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
