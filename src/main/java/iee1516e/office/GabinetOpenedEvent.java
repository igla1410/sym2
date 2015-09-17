package iee1516e.office;

/**
 * Created by piotr on 17.09.2015.
 */
public class GabinetOpenedEvent
{
    private int idGabinet;
    private int registrationLimit;

    public GabinetOpenedEvent(int idGabinet, int registrationLimit)
    {
        this.idGabinet = idGabinet;
        this.registrationLimit = registrationLimit;
    }

    public int getIdGabinet()
    {
        return idGabinet;
    }

    public void setIdGabinet(int idGabinet)
    {
        this.idGabinet = idGabinet;
    }

    public int getRegistrationLimit()
    {
        return registrationLimit;
    }

    public void setRegistrationLimit(int registrationLimit)
    {
        this.registrationLimit = registrationLimit;
    }
}
