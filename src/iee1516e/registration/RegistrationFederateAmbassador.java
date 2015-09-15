package iee1516e.registration;

import hla.rti1516e.*;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.time.HLAfloat64Time;

/**
 * Created by piotr on 14.09.2015.
 */
public class RegistrationFederateAmbassador extends NullFederateAmbassador
{
    private RegistrationFederate federate;
    protected double federateTime = 0.0;
    protected double federateLookahead = 1.0;

    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;

    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;
    protected boolean running = true;

    public RegistrationFederateAmbassador(RegistrationFederate federate)
    {
        this.federate = federate;
    }

    private void log(String message)
    {
        System.out.println("czas: " + federate.getTimeAsShort() + " - FederateAmbassador: " + message);
    }

    public void synchronizationPointRegistrationFailed(String label, SynchronizationPointFailureReason reason)
    {
        log("Failed to register sync point: " + label + ", reason=" + reason);
    }

    public void synchronizationPointRegistrationSucceeded(String label)
    {
        log("Successfully registered sync point: " + label);
    }

    public void announceSynchronizationPoint(String label, byte[] tag)
    {
        log("Synchronization point announced: " + label);
        if (label.equals(RegistrationFederate.READY_TO_RUN))
            this.isAnnounced = true;
    }

    public void federationSynchronized(String label, FederateHandleSet failed)
    {
        log("Federation Synchronized: " + label);
        if (label.equals(RegistrationFederate.READY_TO_RUN))
            this.isReadyToRun = true;
    }

    public void timeRegulationEnabled(LogicalTime time)
    {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isRegulating = true;
    }

    public void timeConstrainedEnabled(LogicalTime time)
    {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isConstrained = true;
    }

    public void timeAdvanceGrant(LogicalTime time)
    {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isAdvancing = false;
    }

    public void discoverObjectInstance(ObjectInstanceHandle theObject, ObjectClassHandle theObjectClass, String objectName) throws FederateInternalError
    {
        log("Discovered Object: handle=" + theObject + ", classHandle=" +
                theObjectClass + ", name=" + objectName);
    }

    public void reflectAttributeValues(ObjectInstanceHandle theObject, AttributeHandleValueMap theAttributes, byte[] tag, OrderType sentOrder, TransportationTypeHandle transport,
                                       SupplementalReflectInfo reflectInfo) throws FederateInternalError
    {

        reflectAttributeValues(theObject, theAttributes, tag, sentOrder, transport, null, sentOrder, reflectInfo);
    }

    public void reflectAttributeValues(ObjectInstanceHandle theObject,
                                       AttributeHandleValueMap theAttributes,
                                       byte[] tag,
                                       OrderType sentOrdering,
                                       TransportationTypeHandle theTransport,
                                       LogicalTime time,
                                       OrderType receivedOrdering,
                                       SupplementalReflectInfo reflectInfo)
            throws FederateInternalError
    {
    }

    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters,
                                   byte[] tag,
                                   OrderType sentOrdering,
                                   TransportationTypeHandle theTransport,
                                   SupplementalReceiveInfo receiveInfo)
            throws FederateInternalError
    {

        this.receiveInteraction(interactionClass,
                theParameters,
                tag,
                sentOrdering,
                theTransport,
                null,
                sentOrdering,
                receiveInfo);
    }

    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters,
                                   byte[] tag,
                                   OrderType sentOrdering,
                                   TransportationTypeHandle theTransport,
                                   LogicalTime time,
                                   OrderType receivedOrdering,
                                   SupplementalReceiveInfo receiveInfo)
            throws FederateInternalError
    {
       /* try {
            if (interactionClass.equals(RestauracjaFederate.rtiamb.getInteractionClassHandle("HLAinteractionRoot.zamkniecieRestauracji"))){
                for (ParameterHandle parameter : theParameters.keySet()) {
                    if (parameter.equals(RestauracjaFederate.rtiamb.getParameterHandle(interactionClass, "typKomunikatu"))) {
                        int typKomunikatu = iee1516e.utils.DecoderUtils.decodeInteger(theParameters.get(parameter));
                        if (typKomunikatu == 2) {
                            log("KONIEC!");
                            running = false;
                        }
                    }
                }
            }
        } catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError | InvalidInteractionClassHandle nameNotFound) {
            nameNotFound.printStackTrace();
        }*/
    }

    public void removeObjectInstance(ObjectInstanceHandle theObject,
                                     byte[] tag,
                                     OrderType sentOrdering,
                                     SupplementalRemoveInfo removeInfo)
            throws FederateInternalError
    {
        log("Object Removed: handle=" + theObject);
    }
}

