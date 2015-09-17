package iee1516e.waitingroom;

import hla.rti1516e.*;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Time;
import iee1516e.office.OfficeFederate;
import iee1516e.registration.RegistrationFederate;
import iee1516e.utils.DecoderUtils;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by piotr on 16.09.2015.
 */
public class WaitingRoomFederateAmbassador extends NullFederateAmbassador
{
    private WaitingRoomFederate federate;
    protected double federateTime = 0.0;
    protected double federateLookahead = 1.0;

    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;

    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;
    protected boolean running = true;

    public WaitingRoomFederateAmbassador(WaitingRoomFederate federate)
    {
        this.federate = federate;
    }

    private void log(String message)
    {
        System.out.println("czas: " + federate.getTimeAsShort() + " - FederateAmbassador: " + message);
    }

    @Override
    public void synchronizationPointRegistrationFailed(String label, SynchronizationPointFailureReason reason)
    {
        log("Failed to register sync point: " + label + ", reason=" + reason);
    }

    @Override
    public void synchronizationPointRegistrationSucceeded(String label)
    {
        log("Successfully registered sync point: " + label);
    }

    @Override
    public void announceSynchronizationPoint(String label, byte[] tag)
    {
        log("Synchronization point announced: " + label);
        if (label.equals(WaitingRoomFederate.READY_TO_RUN))
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failed)
    {
        log("Federation Synchronized: " + label);
        if (label.equals(WaitingRoomFederate.READY_TO_RUN))
            this.isReadyToRun = true;
    }

    @Override
    public void timeRegulationEnabled(LogicalTime time)
    {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isRegulating = true;
    }

    @Override
    public void timeConstrainedEnabled(LogicalTime time)
    {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isConstrained = true;
    }

    @Override
    public void timeAdvanceGrant(LogicalTime time)
    {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isAdvancing = false;
    }

    @Override
    public void discoverObjectInstance(ObjectInstanceHandle theObject, ObjectClassHandle theObjectClass, String objectName) throws FederateInternalError
    {
        log("Discoverd Object: handle=" + theObject + ", classHandle=" +
                theObjectClass + ", name=" + objectName);
    }

    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject, AttributeHandleValueMap theAttributes, byte[] tag, OrderType sentOrder, TransportationTypeHandle transport,
                                       FederateAmbassador.SupplementalReflectInfo reflectInfo) throws FederateInternalError
    {

        reflectAttributeValues(theObject, theAttributes, tag, sentOrder, transport, null, sentOrder, reflectInfo);
    }

    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject,
                                       AttributeHandleValueMap theAttributes,
                                       byte[] tag,
                                       OrderType sentOrdering,
                                       TransportationTypeHandle theTransport,
                                       LogicalTime time,
                                       OrderType receivedOrdering,
                                       FederateAmbassador.SupplementalReflectInfo reflectInfo)
            throws FederateInternalError
    {

    }

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters,
                                   byte[] tag,
                                   OrderType sentOrdering,
                                   TransportationTypeHandle theTransport,
                                   FederateAmbassador.SupplementalReceiveInfo receiveInfo)
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

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters,
                                   byte[] tag,
                                   OrderType sentOrdering,
                                   TransportationTypeHandle theTransport,
                                   LogicalTime time,
                                   OrderType receivedOrdering,
                                   FederateAmbassador.SupplementalReceiveInfo receiveInfo)
            throws FederateInternalError
    {
        try
        {
            if (interactionClass.equals(WaitingRoomFederate.rtiamb.getInteractionClassHandle("HLAinteractionRoot.patientWaiting")))
            {
                int idGabinet = 0;
                int idPatient = 0;


                for (ParameterHandle parameterHandle : theParameters.keySet())
                {
                    if (parameterHandle.equals(WaitingRoomFederate.rtiamb.getParameterHandle(interactionClass, "idGabinet")))
                    {
                        idGabinet = DecoderUtils.decodeInteger(theParameters.get(parameterHandle));
                    }
                    else if (parameterHandle.equals(WaitingRoomFederate.rtiamb.getParameterHandle(interactionClass, "idPatient")))
                    {
                        idPatient = DecoderUtils.decodeInteger(theParameters.get(parameterHandle));
                    }
                }
                federate.queues.get(idGabinet).add(idPatient);
                //log(idGabinet + " " + idPatient);
            }
            else if (interactionClass.equals(WaitingRoomFederate.rtiamb.getInteractionClassHandle("HLAinteractionRoot.gabinetOpened")))
            {
                int idGabinet = 0;
                for (ParameterHandle parameterHandle : theParameters.keySet())
                {
                    if (parameterHandle.equals(WaitingRoomFederate.rtiamb.getParameterHandle(interactionClass, "idGabinet")))
                    {
                        idGabinet = DecoderUtils.decodeInteger(theParameters.get(parameterHandle));
                    }
                }
                log("Odkry³em, ¿e otwarto gabinet= " + idGabinet);
                federate.queues.put(Integer.valueOf(idGabinet), new LinkedList<>());
                federate.freeGabinets.put(Integer.valueOf(idGabinet), true);
                federate.gabinetOpened = true;
            }
            else if (interactionClass.equals(WaitingRoomFederate.rtiamb.getInteractionClassHandle("HLAinteractionRoot.gabinetStatusChanged")))
            {
                int idGabinet = 0;
                int state = 0;
                for (ParameterHandle parameterHandle : theParameters.keySet())
                {
                    if (parameterHandle.equals(WaitingRoomFederate.rtiamb.getParameterHandle(interactionClass, "idGabinet")))
                    {
                        idGabinet = DecoderUtils.decodeInteger(theParameters.get(parameterHandle));
                    }
                    else if (parameterHandle.equals(WaitingRoomFederate.rtiamb.getParameterHandle(interactionClass, "state")))
                    {
                        state = DecoderUtils.decodeInteger(theParameters.get(parameterHandle));
                    }
                    if (state == 0)
                        federate.freeGabinets.replace(idGabinet, true);
                    else
                        federate.freeGabinets.replace(idGabinet, false);
                    log("Odkry³em zmianê stanu gabinetu=" + idGabinet + " na " + (idGabinet == 0 ? "wolny" : "zajêty/przerwa"));
                }
            }
            else if (interactionClass.equals(WaitingRoomFederate.rtiamb.getInteractionClassHandle("HLAinteractionRoot.workStatus")))
            {
                int status=0;
                for (ParameterHandle parameterHandle:theParameters.keySet())
                {
                    if (parameterHandle.equals(WaitingRoomFederate.rtiamb.getParameterHandle(interactionClass, "typKomunikatu")))
                    {
                        status = DecoderUtils.decodeInteger(theParameters.get(parameterHandle));
                        if (status==-1)
                        {
                            log("Zamykamy przychodniê.");
                            running=false;
                        }
                    }

                }
            }
        }
        catch (NameNotFound | FederateNotExecutionMember | NotConnected | RTIinternalError | InvalidInteractionClassHandle nameNotFound)
        {
            nameNotFound.printStackTrace();
        }
    }

    @Override
    public void removeObjectInstance(ObjectInstanceHandle theObject,
                                     byte[] tag,
                                     OrderType sentOrdering,
                                     FederateAmbassador.SupplementalRemoveInfo removeInfo)
            throws FederateInternalError
    {
        log("Object Removed: handle=" + theObject);
    }
}


