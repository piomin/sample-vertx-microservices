package pl.piomin.services.vertx.account;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class AccountServerTest {

	@Rule
	public RunTestOnContext rule = new RunTestOnContext();

	@Test
	public void testSomething(TestContext context) {
		context.assertFalse(false);
	}

}
