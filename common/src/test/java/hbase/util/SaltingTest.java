/* project-big-data-processing-pipeline
 *
 * Created: 29/1/22 6:23 pm
 *
 * Description:
 */
package hbase.util;

import org.junit.Assert;
import org.junit.Test;

public class SaltingTest {

    @Test
    public void shouldReturnCorrectSaltedKey() {
        String saltedKey = Salting.getSaltedKey("VN2021-08-04");
        Assert.assertEquals("3d:VN2021-08-04", saltedKey);
    }

    @Test
    public void shouldReturnCorrectSaltedKey1() {
        String saltedKey = Salting.getSaltedKey("ID2021-08-04");
        Assert.assertEquals("52:ID2021-08-04", saltedKey);
    }

    @Test
    public void shouldReturnCorrectSaltedKey2() {
        String saltedKey = Salting.getSaltedKey("ID2021-08-04", 5);
        Assert.assertEquals("36352:ID2021-08-04", saltedKey);
    }

}