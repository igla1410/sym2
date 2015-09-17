package iee1516e.office;

/**
 * Created by piotr on 17.09.2015.
 */
public class PatientGabinet
{
    int idPatient;
    int idGabinet;

    public PatientGabinet(int idPatient, int idGabinet)
    {
        this.idPatient = idPatient;
        this.idGabinet = idGabinet;
    }

    public int getIdPatient()
    {
        return idPatient;
    }

    public void setIdPatient(int idPatient)
    {
        this.idPatient = idPatient;
    }

    public int getIdGabinet()
    {
        return idGabinet;
    }

    public void setIdGabinet(int idGabinet)
    {
        this.idGabinet = idGabinet;
    }
}
