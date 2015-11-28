import org.junit.Test;

public class misc {
    @Test
    public void testMisc(){
        String msg = "a\"    \"\"   b";
        System.out.println(msg.replaceAll("\\s+|\""," "));
    }

}
