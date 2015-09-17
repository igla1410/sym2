package iee1516e.registration;

import hla.rti1516e.*;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Time;
import iee1516e.office.Office;
import iee1516e.patient.Patient;
import iee1516e.patient.PatientFederate;
import iee1516e.utils.DecoderUtils;

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

    private Patient getPatient(ObjectInstanceHandle theObject)
    {
        if (!federate.patientsToRegister.containsKey(theObject))
        {
            Patient patient = new Patient();
            federate.patientsToRegister.put(theObject, patient);
        }
        return federate.patientsToRegister.get(theObject);
    }

/*    private Office getOffice(ObjectInstanceHandle theObject)
    {
        if (!federate.gabinets.containsKey(theObject))
        {
            Office office = new Office();
            federate.gabinets.put(theObject, office);
        }
        return federate.gabinets.get(theObject);
    }*/

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
        try
        {
            ObjectClassHandle patientHandle = RegistrationFederate.rtiamb.getObjectClassHandle("HLAobjectRoot.Patient");
            ObjectClassHandle gabinetHandle = RegistrationFederate.rtiamb.getObjectClassHandle("HLAobjectRoot.Gabinet");
            for (AttributeHandle attributeHandle : theAttributes.keySet())
            {
                if (attributeHandle.equals(RegistrationFederate.rtiamb.getAttributeHandle(patientHandle, "doctorPreference")))
                {
                    Integer doctorPreference = DecoderUtils.decodeInteger(theAttributes.get(attributeHandle));
                    Patient patient = getPatient(theObject);
                    patient.setDoctorPreference(doctorPreference.intValue());


                    log("Do rejestracji przyby³ pacjent do lekarza: " + doctorPreference);

                }
                else if (attributeHandle.equals(RegistrationFederate.rtiamb.getAttributeHandle(patientHandle, "cito")))
                {
                    Boolean cito = DecoderUtils.decodeBoolean(theAttributes.get(attributeHandle));
                    Patient patient = getPatient(theObject);
                    patient.setCito(cito.booleanValue());
                    //federate.patientsToRegister.put(theObject, patient);
                }
                else if (attributeHandle.equals(RegistrationFederate.rtiamb.getAttributeHandle(patientHandle, "idPatient")))
                {
                    Integer id = DecoderUtils.decodeInteger(theAttributes.get(attributeHandle));
                    Patient patient = getPatient(theObject);
                    patient.setIdPatient(id.intValue());
                }
/*                else if (attributeHandle.equals(RegistrationFederate.rtiamb.getAttributeHandle(gabinetHandle, "idGabinet")))
                {
                    Integer gabinetId = DecoderUtils.decodeInteger(theAttributes.get(attributeHandle));
                    Office office = getOffice(theObject);
                    office.setOfficeId(gabinetId.intValue());
                    log("aaaa");
                }
                else if (attributeHandle.equals(RegistrationFederate.rtiamb.getAttributeHandle(gabinetHandle, "registrationLimit")))
                    ;
                {
                    Integer registrationLimit = DecoderUtils.decodeInteger(theAttributes.get(attributeHandle));
                    Office office = getOffice(theObject);
                    office.setRegistrationLimit(registrationLimit.intValue());
                    // federate.gabinets.put(theObject,office);
                    log("Otrzyma³em informacjê, ¿e lekarz ma limit przyjêæ=" + registrationLimit);
                    log("aaaa");
                }*/
            }

        }
        catch (RTIexception rtIexception)
        {
            rtIexception.printStackTrace();
        }
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
        try
        {

            if (interactionClass.equals(RegistrationFederate.rtiamb.getInteractionClassHandle("HLAinteractionRoot.gabinetOpened")))
            {
                int idGabinet=0;
                int registrationLimit=0;
                for (ParameterHandle parameterHandle:theParameters.keySet())
                {
                    if (parameterHandle.equals(RegistrationFederate.rtiamb.getParameterHandle(interactionClass, "idGabinet")))
                    {
                        idGabinet = DecoderUtils.decodeInteger(theParameters.get(parameterHandle));
                    }
                    else if (parameterHandle.equals(RegistrationFederate.rtiamb.getParameterHandle(interactionClass, "registrationLimit")))
                    {
                        registrationLimit=DecoderUtils.decodeInteger(theParameters.get(parameterHandle));
                    }
                }
                log("Odkry³em, ¿e otwarto gabinet= "+idGabinet+" "+" o limicie przyjêæ "+registrationLimit);
                federate.registrationLimits.put(Integer.valueOf(idGabinet), Integer.valueOf(registrationLimit));
                federate.patientsRegistered.put(Integer.valueOf(idGabinet),0);
            }
           else if (interactionClass.equals(RegistrationFederate.rtiamb.getInteractionClassHandle("HLAinteractionRoot.workStatus")))
            {
                int status=0;
                for (ParameterHandle parameterHandle:theParameters.keySet())
                {
                    if (parameterHandle.equals(RegistrationFederate.rtiamb.getParameterHandle(interactionClass, "typKomunikatu")))
                    {
                        status = DecoderUtils.decodeInteger(theParameters.get(parameterHandle));
                        if (status==0)
                        {
                            log("Zamykamy przychodniê (lekarze maj¹ nadal pacjentów)");
                            running=false;
                        }
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

    public void removeObjectInstance(ObjectInstanceHandle theObject,
                                     byte[] tag,
                                     OrderType sentOrdering,
                                     SupplementalRemoveInfo removeInfo)
            throws FederateInternalError
    {
        log("Object Removed: handle=" + theObject);
    }
}

