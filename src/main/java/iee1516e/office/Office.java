package iee1516e.office;

/**
 * Created by piotr on 15.09.2015.
 */
public class Office
{


    public enum STATE
    {
        FREE, BUSY, BREAK
    }

    ;

    public Office(int officeId, int idPatient, STATE state, int registrationLimit)
    {
        this.officeId = officeId;
        this.idPatient = idPatient;
        this.state = state;
        this.registrationLimit = registrationLimit;
    }

    public Office()
    {

    }

    public Office(int registrationLimit)
    {
        this.registrationLimit = registrationLimit;
    }

    public Office(int idPatient, STATE state)
    {
        this.idPatient = idPatient;
        this.state = state;
    }

    public boolean isBusy()
    {
        if (state != STATE.FREE)
            return true;
        return false;

    }

    public int getIdPatient()
    {
        return idPatient;
    }

    public void setIdPatient(int idPatient)
    {
        this.idPatient = idPatient;
    }

    public STATE getState()
    {
        return state;
    }

    public void setState(STATE state)
    {
        this.state = state;
    }

    public int getOfficeId()
    {
        return officeId;
    }

    public void setOfficeId(int officeId)
    {
        this.officeId = officeId;
    }

    private int officeId;
    private int idPatient;
    private STATE state;
    private int registrationLimit;

    public int getRegistrationLimit()
    {
        return registrationLimit;
    }

    public void setRegistrationLimit(int registrationLimit)
    {
        this.registrationLimit = registrationLimit;
    }
}
