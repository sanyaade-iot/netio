package at.creadoo.util.netio;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import at.creadoo.util.netio.NetIOBuilder;
import at.creadoo.util.netio.NetIOException;
import at.creadoo.util.netio.NetIO;
import at.creadoo.util.netio.State;

public class NetIOTest {

	private NetIO netIO;

	@Mock
	private BufferedWriter writer;

	@Mock
	private BufferedReader reader;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		netIO = new NetIOBuilder("hostname", 1234).setUsername("username").setPassword("password").build();
		netIO.writer = writer;
		netIO.reader = reader;
	}

	@Test
	public void checkIsDisconnected() {
		assertEquals(State.DISCONNECTED, netIO.state);
	}

	@Test(expected = NetIOException.class)
	public void checkNullStringCommandRaisesException() throws NetIOException {
		netIO.command(null);
	}

	@Test(expected = NetIOException.class)
	public void checkUndefinedCommandRaisesException() throws NetIOException {
		netIO.command("undefined");
	}

	@Test
	public void checkNormalCommandIsWritten() throws NetIOException, IOException {
		netIO.state = State.AUTHORIZED;
		netIO.setPorts("01iu");
		verify(writer).write("port list 01iu");
	}
}
