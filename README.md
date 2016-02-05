# android_api
The API uses the following trilateration library (https://github.com/lemmingapex/Trilateration). This relies on the org.apache.commons.math3 library to work (http://commons.apache.org/proper/commons-math/), so you need to include this library in your project.

Please note that this is a proof of concept demonstator that implements the SubPos standard (http://wiki.subpos.org/index.php?title=SubPos_Standard) and a method of trilateration, but work is being made to make this more robust and configurable. Please feel free to suggest any additions or fixes.

To use this API, all you need to do is call:

    public class yourClass extends Activity {
        SubPos subpos;
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.main);
    
            //Create new SubPos service
            subpos = new SubPos(this); //"this" passes the context
        }

To get your position:

    SubPosPosition position = subpos.getPosition(); //returns null if position is not calculated
