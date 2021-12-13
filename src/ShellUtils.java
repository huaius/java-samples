package src;
	 
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
	 
import org.apache.commons.io.IOUtils;
	 
import lombok.SneakyThrows;
	 
public class ShellUtils {
    private ShellUtils() {
	     }
	 
	@SneakyThrows
	public static String getShellOutput(final String ...command) {
	    Process p = new ProcessBuilder(command)
	                 .redirectInput(Redirect.INHERIT)
	                 .redirectError(Redirect.INHERIT)
	                 .start();
	    return IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
    }
}
