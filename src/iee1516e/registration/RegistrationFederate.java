package iee1516e.registration;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

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

    public void runFederate(String federateName) throws Exception
    {
        publishAndSubscribe();

    }

    private void publishAndSubscribe() throws RTIexception
    {

    }

    private void destroyFederate(List<ObjectInstanceHandle> clients) throws RTIexception
    {
        //Thread.sleep(1000);
        for (ObjectInstanceHandle oh : clients)
        {
            deleteObject(oh);
            log("Deleted Object, handle=" + oh);
        }

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
