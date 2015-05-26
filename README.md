# android_api
In the open source API I have moved to another trilateration library from here (https://github.com/lemmingapex/Trilateration). This relies on the org.apache.commons.math3 library to work (http://commons.apache.org/proper/commons-math/), so you need to include this library in your project.

This initial release has been adapted from the prototype code that has been worked on, so bear with me if there are some errors or omissions, but I have tried to make it simple to use.

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
