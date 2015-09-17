package iee1516e.waitingroom;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import iee1516e.office.Office;
import iee1516e.office.OfficeFederate;
import org.apache.commons.collections.MultiHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by piotr on 16.09.2015.
 */
public class WaitingRoomFederate
{
    public static final String READY_TO_RUN = "ReadyToRun";
    public static RTIambassador rtiamb;
    private WaitingRoomFederateAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory;
    protected EncoderFactory encoderFactory;
    Map<Integer,LinkedList<Integer>> queues=new LinkedHashMap<>();
    Map<Integer, Boolean> freeGabinets =new LinkedHashMap<>();
    boolean gabinetOpened=true;
    public void runFederate(String federateName) throws Exception
    {
        if (prepareFederate(federateName)) return;
        publishAndSubscribe();
        prepareQueues();
        while (fedamb.running)
        {
            if (gabinetOpened)
                sendReady();
            advanceTime(1.0);

        }
        destroyFederate();
    }
    private ObjectInstanceHandle registerObject() throws RTIexception
    {
        ObjectClassHandle gabinetHandle=rtiamb.getObjectClassHandle("HLAobjectRoot.Gabinet");
        return rtiamb.registerObjectInstance(gabinetHandle);
    }
    private void sendReady() throws RTIexception
    {
        for (Map.Entry<Integer, Boolean> gabinet: freeGabinets.entrySet())
        {
            boolean isFree=Boolean.valueOf(gabinet.getValue());
            Integer key=gabinet.getKey();
            if (isFree && !queues.get(key).isEmpty())
            {
                Integer patientId=queues.get(key).poll();
                sendPatientReadyInteraction(patientId.intValue(),key.intValue());
                freeGabinets.replace(key,false);
            }
        }
    }
    private void sendPatientReadyInteraction(int idPatient, int idGabinet)throws RTIexception
    {
        InteractionClassHandle patientReadyHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.patientReady");
        ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);
        ParameterHandle idPatientHandle=rtiamb.getParameterHandle(patientReadyHandle, "idPatient");
        HLAinteger32BE patientId=encoderFactory.createHLAinteger32BE(idPatient);
        parameters.put(idPatientHandle,patientId.toByteArray());

        ParameterHandle idGabinetHandle=rtiamb.getParameterHandle(patientReadyHandle,"idGabinet");
        HLAinteger32BE idGabinetHLA=encoderFactory.createHLAinteger32BE(idGabinet);
        parameters.put(idGabinetHandle,idGabinetHLA.toByteArray());

        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + fedamb.federateLookahead);
        rtiamb.sendInteraction(patientReadyHandle,parameters,generateTag(),time);
    }

    private void publishAndSubscribe() throws RTIexception
    {
        InteractionClassHandle gabinetOpenedHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.gabinetOpened");
        rtiamb.subscribeInteractionClass(gabinetOpenedHandle);
        /*InteractionClassHandle inGabinetHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.inGabinet");
        rtiamb.publishInteractionClass(inGabinetHandle);*/
        InteractionClassHandle inWaitingRoomHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.patientWaiting");
        rtiamb.subscribeInteractionClass(inWaitingRoomHandle);
        InteractionClassHandle patientReadyRoomHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.patientReady");
        rtiamb.publishInteractionClass(patientReadyRoomHandle);
        InteractionClassHandle gabinetStatusChangedHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.gabinetStatusChanged");
        rtiamb.subscribeInteractionClass(gabinetStatusChangedHandle);
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
        rtiamb.subscribeObjectClassAttributes(gabinetHandle,attributes);*/
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
    public void prepareQueues()
    {
        for (int i=0;i< OfficeFederate.COUNT;i++)
        {
            queues.put(i,new LinkedList<>());
            log("Utworzono kolejkê do gabinetu "+i);
        }
    }
    private boolean prepareFederate(String federateName) throws Exception
    {
        log("Creating RTIambassador");
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

        log("Connecting...");
        fedamb = new WaitingRoomFederateAmbassador(this);
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
                "WaitingRoomFederateType",   // federate type
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
    public static void main(String[] args)
    {

        String federateName = "WaitingRoomFederate";
        if (args.length != 0)
        {
            federateName = args[0];
        }
        try
        {
            new WaitingRoomFederate().runFederate(federateName);
        }
        catch (Exception rtie)
        {
            rtie.printStackTrace();
        }
    }

    private void log(String message)
    {
        System.out.println("czas: " + getTimeAsShort() + " - WaitingRoomFederate   : " + message);
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