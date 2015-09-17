package iee1516e.patient;

/**
 * Created by piotr on 17.09.2015.
 */
public class PatientEvent
{
    public PatientEvent(int idPatient, long time)
    {
        this.idPatient = idPatient;
        this.time = time;
    }

    private int idPatient;
    private long time;

    public int getIdPatient()
    {
        return idPatient;
    }

    public void setIdPatient(int idPatient)
    {
        this.idPatient = idPatient;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }
}
