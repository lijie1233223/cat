package com.dianping.cat.storage;

import java.io.File;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.dianping.cat.message.spi.MessageTree;
import com.dianping.cat.message.spi.internal.DefaultMessageTree;
import com.site.lookup.ComponentTestCase;

@RunWith(JUnit4.class)
public class BucketTest extends ComponentTestCase {
	@BeforeClass
	public static void beforeClass() {
		new File("target/bucket/bytes").delete();
		new File("target/bucket/message").delete();
		new File("target/bucket/data").delete();
	}

	private DefaultMessageTree newMessageTree(String id) {
		DefaultMessageTree tree = new DefaultMessageTree();

		tree.setDomain("domain");
		tree.setHostName("hostName");
		tree.setIpAddress("ipAddress");
		tree.setMessageId(id);
		tree.setParentMessageId("parentMessageId");
		tree.setRootMessageId("rootMessageId");
		tree.setSessionToken("sessionToken");
		tree.setThreadId("threadId");
		tree.setThreadName("threadName");

		return tree;
	}

	@Test
	public void testBytesBucket() throws Exception {
		BucketManager manager = lookup(BucketManager.class);
		Bucket<byte[]> bucket = manager.getBytesBucket("bytes");

		// store it and load it
		for (int i = 0; i < 100; i++) {
			String id = "id" + i;
			String t1 = "value" + i;
			boolean success = bucket.storeById(id, t1.getBytes());

			if (success) {
				String t2 = new String(bucket.findById(id));

				Assert.assertEquals("Unable to find data after stored it.", t1, t2);
			} else {
				Assert.fail("Data failed to store at i=" + i + ".");
			}
		}

		// close and reload it, check if everything is okay
		bucket.close();
		bucket.initialize(byte[].class, new File("target/bucket/"), "bytes");

		// store it and load it
		for (int i = 0; i < 100; i++) {
			String id = "id" + i;
			String t1 = "value" + i;
			String t2 = new String(bucket.findById(id));

			Assert.assertEquals("Unable to find data by id.", t1, t2);
		}
	}

	@Test
	public void testMessageBucket() throws Exception {
		BucketManager manager = lookup(BucketManager.class);
		Bucket<MessageTree> bucket = manager.getMessageBucket("message");
		int groups = 10;

		// store it and load it
		for (int i = 0; i < 100; i++) {
			String id = "id" + i;
			MessageTree t1 = newMessageTree(id);
			boolean success = bucket.storeById(id, t1, "r:" + (i % groups));

			if (success) {
				MessageTree t2 = bucket.findById(id);

				Assert.assertEquals("Unable to find message after stored it.", t1.toString(), t2.toString());
			} else {
				Assert.fail("Message failed to store at i=" + i + ".");
			}
		}

		// check next message in the same thread
		for (int i = 0; i < groups - 1; i++) {
			String id = "id" + (i * groups + i);
			String nextId = "id" + ((i + 1) * groups + i);
			String tag = "r:" + i;
			MessageTree t1 = bucket.findNextById(id, tag);
			MessageTree t2 = bucket.findById(nextId);

			Assert.assertEquals("Unable to find next message in the thread " + i + ".", t1.toString(), t2.toString());
		}

		// close and reload it, check if everything is okay
		bucket.close();
		bucket.initialize(MessageTree.class, new File("target/bucket/"), "message");

		// check next message in the same thread
		for (int i = 0; i < groups - 1; i++) {
			String id = "id" + (i * groups + i);
			String nextId = "id" + ((i + 1) * groups + i);
			String tag = "r:" + i;
			MessageTree t1 = bucket.findNextById(id, tag);
			MessageTree t2 = bucket.findById(nextId);

			Assert.assertEquals("Unable to find next message in the thread " + i + ".", t1.toString(), t2.toString());
		}
	}

	@Test
	public void testStringBucket() throws Exception {
		BucketManager manager = lookup(BucketManager.class);
		Bucket<String> bucket = manager.getStringBucket("data");

		// store it and load it
		for (int i = 0; i < 100; i++) {
			String id = "id" + i;
			String t1 = "value" + i;
			boolean success = bucket.storeById(id, t1);

			if (success) {
				String t2 = bucket.findById(id);

				Assert.assertEquals("Unable to find data after stored it.", t1, t2);
			} else {
				Assert.fail("Data failed to store at i=" + i + ".");
			}
		}

		// close and reload it, check if everything is okay
		bucket.close();
		bucket.initialize(String.class, new File("target/bucket/"), "data");

		// store it and load it
		for (int i = 0; i < 100; i++) {
			String id = "id" + i;
			String t1 = "value" + i;
			String t2 = bucket.findById(id);

			Assert.assertEquals("Unable to find data by id.", t1, t2);
		}

		for (int i = 0; i < 90; i++) {
			String id = "id" + i;
			String t1 = "value" + (i + 10);
			String tag = "tag" + (i % 10);
			String t2 = bucket.findNextById(id, tag);

			Assert.assertEquals("Unable to find data by id.", t1, t2);
		}

		for (int i = 10; i < 100; i++) {
			String id = "id" + i;
			String t1 = "value" + (i - 10);
			String tag = "tag" + (i % 10);
			String t2 = bucket.findPreviousById(id, tag);

			Assert.assertEquals("Unable to find data by id.", t1, t2);
		}

	}
}
