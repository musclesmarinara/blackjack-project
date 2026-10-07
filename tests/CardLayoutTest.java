import com.example.blackjackoverlay.CardLayout;
public class CardLayoutTest {
    static int n;static void check(boolean b){n++;if(!b)throw new AssertionError("Layout check "+n);}
    static boolean near(float a,float b){return Math.abs(a-b)<.00001f;}
    public static void main(String[] args){
        CardLayout c=new CardLayout();
        for(int h=0;h<7;h++)for(int k=0;k<5;k++){float[] q=c.corners(h,k,945,2048);check(q.length==8);for(int i=0;i<8;i+=2)check(q[i]>=0&&q[i]<945&&q[i+1]>=0&&q[i+1]<2048);if(k>0)check(c.slots[h][k][1]<c.slots[h][k-1][1]);}
        for(int h=1;h<7;h++)check(c.slots[h][0][0]>c.slots[h-1][0][0]);
        CardLayout old=c.copy();c.slots[0][0][0]+=.01f;check(near(old.slots[0][0][0],171/945f));check(near(c.slots[1][0][0],old.slots[1][0][0]));
        c=old.copy();float[] b=c.bounds(3);c.transformHand(3,b[0]+.02f,b[1]+.01f,b[2]+.02f,b[3]+.01f);
        for(int k=0;k<5;k++){check(near(c.slots[3][k][0],old.slots[3][k][0]+.02f));check(near(c.slots[3][k][1],old.slots[3][k][1]+.01f));check(near(c.slots[3][k][2],old.slots[3][k][2]));}
        c=old.copy();c.rotate(2,1,5,945f/2048);check(near(c.slots[2][1][4],old.slots[2][1][4]+5));check(near(c.slots[2][0][4],old.slots[2][0][4]));
        c=old.copy();c.rotate(4,-1,15,945f/2048);c.rotate(4,-1,-15,945f/2048);for(int k=0;k<5;k++){check(near(c.slots[4][k][0],old.slots[4][k][0]));check(near(c.slots[4][k][1],old.slots[4][k][1]));}
        System.out.println("PASS: "+n+" layout assertions");
    }
}
