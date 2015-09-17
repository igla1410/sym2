package iee1516e.patient;

/**
 * Created by pitt on 14.09.2015.
 */
public class Patient
{
    private int idPatient;
    private boolean cito = false;
    private int doctorPreference;
    private short registrationTime = -1;
    private short waitingTime = -1;
    private boolean registered = false;

    public boolean isRegistered()
    {
        return registered;
    }

    public void setRegistered(boolean registered)
    {
        this.registered = registered;
    }

    public int getIdPatient()
    {
        return idPatient;
    }

    public void setIdPatient(int idPatient)
    {
        this.idPatient = idPatient;
    }

    public boolean isCito()
    {
        return cito;
    }

    public void setCito(boolean cito)
    {
        this.cito = cito;
    }

    public int getDoctorPreference()
    {
        return doctorPreference;
    }

    public void setDoctorPreference(int doctorPreference)
    {
        this.doctorPreference = doctorPreference;
    }

    public short getRegistrationTime()
    {
        return registrationTime;
    }

    public void setRegistrationTime(short registrationTime)
    {
        this.registrationTime = registrationTime;
    }

    public short getWaitingTime()
    {
        return waitingTime;
    }

    public void setWaitingTime(short waitingTime)
    {
        this.waitingTime = waitingTime;
    }
}
