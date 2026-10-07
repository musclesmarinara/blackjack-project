import com.example.blackjackoverlay.ScanControl;
import com.example.blackjackoverlay.RankFilter;
public class ScannerTest {
    static int n;static void check(boolean b){n++;if(!b)throw new AssertionError("Scanner check "+n);}
    public static void main(String[] args){
        ScanControl s=new ScanControl();check(!s.running());check(!s.accept(s.token()));int first=s.start();check(s.running());check(s.accept(first));s.stop();check(!s.running());check(!s.accept(first));int second=s.start();check(s.accept(second));check(!s.accept(first));s.stop();s.stop();check(!s.running());check(!s.accept(second));
        for(float a:new float[]{0,5,-5,25,-25,55,-55})check(RankFilter.upright(a));
        for(float a:new float[]{56,-56,90,-90,180,-180,Float.NaN,Float.POSITIVE_INFINITY})check(!RankFilter.upright(a));
        System.out.println("PASS: "+n+" scanner/filter checks");
    }
}
