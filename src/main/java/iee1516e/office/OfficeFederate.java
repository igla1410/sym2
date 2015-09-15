package iee1516e.office;

import hla.rti1516e.RTIambassador;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.time.HLAfloat64TimeFactory;

/**
 * Created by piotr on 15.09.2015.
 */
public class OfficeFederate {
    public static final int COUNT = 5;
    public static final String READY_TO_RUN = "ReadyToRun";
    public static RTIambassador rtiamb;
    private OfficeFederateAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory;
    protected EncoderFactory encoderFactory;

}
