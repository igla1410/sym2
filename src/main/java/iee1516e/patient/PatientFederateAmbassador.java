package iee1516e.patient;

import hla.rti1516e.*;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Time;
import iee1516e.registration.RegistrationFederate;
import iee1516e.utils.DecoderUtils;

/**
 * Created by pitt on 14.09.2015.
 */
public class PatientFederateAmbassador extends NullFederateAmbassador
{
    private PatientFederate federate;
    protected double federateTime = 0.0;
    protected double federateLookahead = 1.0;

    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;

    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;
    protected boolean running = true;

    public PatientFederateAmbassador(PatientFederate federate)
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
        if (label.equals(PatientFederate.READY_TO_RUN))
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failed)
    {
        log("Federation Synchronized: " + label);
        if (label.equals(PatientFederate.READY_TO_RUN))
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
                                       SupplementalReflectInfo reflectInfo) throws FederateInternalError
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
                                       SupplementalReflectInfo reflectInfo)
            throws FederateInternalError
    {
        StringBuilder builder = new StringBuilder("Reflection for object:");

        builder.append(" handle=" + theObject);
        builder.append(", tag=" + new String(tag));
        if (time != null)
        {
            builder.append(", time=" + ((HLAfloat64Time) time).getValue());
        }

        builder.append(", attributeCount=" + theAttributes.size());
        builder.append("\n");
        /*for( AttributeHandle attributeHandle : theAttributes.keySet() )
        {
			builder.append( "\tattributeHandle=" );

			if( attributeHandle.equals(federate.flavHandle) )
			{
				builder.append( attributeHandle );
				builder.append( " (Flavor)    " );
				builder.append( ", attributeValue=" );
				builder.append( decodeFlavor(theAttributes.get(attributeHandle)) );
			}
			else if( attributeHandle.equals(federate.cupsHandle) )
			{
				builder.append( attributeHandle );
				builder.append( " (NumberCups)" );
				builder.append( ", attributeValue=" );
				builder.append( decodeNumCups(theAttributes.get(attributeHandle)) );
			}
			else
			{
				builder.append( attributeHandle );
				builder.append( " (Unknown)   " );
			}

			builder.append( "\n" );
		}*/

        log(builder.toString());
    }

    @Override
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

    @Override
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
        try
        {
            /*
                rtiamb.subscribeInteractionClass(workStatusSubHandle);
                InteractionClassHandle inGabinetHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.inGabinet");
                rtiamb.subscribeInteractionClass(inGabinetHandle);
                InteractionClassHandle outGabinetHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.outGabinet");
                rtiamb.subscribeInteractionClass(outGabinetHandle);
                InteractionClassHandle inWaitingRoomHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.patientWaiting");
                rtiamb.subscribeInteractionClass(inWaitingRoomHandle);
                InteractionClassHandle inRegistrationHandle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.inRegistration");
                rtiamb.subscribeInteractionClass(inRegistrationHandle);
             */
            if (interactionClass.equals(PatientFederate.rtiamb.getInteractionClassHandle("HLAinteractionRoot.workStatus")))
            {
                int status = 0;
                for (ParameterHandle parameterHandle : theParameters.keySet())
                {
                    if (parameterHandle.equals(PatientFederate.rtiamb.getParameterHandle(interactionClass, "typKomunikatu")))
                    {
                        status = DecoderUtils.decodeInteger(theParameters.get(parameterHandle));
                        if (status == 1)
                        {
                            log("Wyczerpano limit przyjêæ");
                            running = false;
                        }
                        if (status == 0)
                        {
                            log("Zamykamy przychodniê (lekarze maj¹ nadal pacjentów)");
                            running = false;
                        }
                        if (status == -1)
                        {
                            log("Zamykamy przychodniê.");
                            running = false;
                        }
                    }

                }
            }
            else if (interactionClass.equals(PatientFederate.rtiamb.getInteractionClassHandle("HLAinteractionRoot.inGabinet")))
            {
                int idPatient = 0;
                for (ParameterHandle parameterHandle : theParameters.keySet())
                {
                    if (parameterHandle.equals(PatientFederate.rtiamb.getParameterHandle(interactionClass, "idPatient")))
                    {
                        idPatient = DecoderUtils.decodeInteger(theParameters.get(parameterHandle));
                    }

                }
                federate.inGabinetEvents.add(new PatientEvent(idPatient,(long) ((HLAfloat64Time) time).getValue()));
                
            }
            else if (interactionClass.equals(PatientFederate.rtiamb.getInteractionClassHandle("HLAinteractionRoot.patientWaiting")))
            {
                int idPatient = 0;
                for (ParameterHandle parameterHandle : theParameters.keySet())
                {
                    if (parameterHandle.equals(PatientFederate.rtiamb.getParameterHandle(interactionClass, "idPatient")))
                    {
                        idPatient = DecoderUtils.decodeInteger(theParameters.get(parameterHandle));
                    }

                }
                federate.patientWaitingEvents.add(new PatientEvent(idPatient,(long) ((HLAfloat64Time) time).getValue()));

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
                                     SupplementalRemoveInfo removeInfo)
            throws FederateInternalError
    {
        log("Object Removed: handle=" + theObject);
    }
}
