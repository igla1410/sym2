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

    private int idPatient;
    private STATE state;

}
