package io.github.donyxu.alpharacks;

import java.io.IOException;
import java.io.PrintStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.net.ssl.SSLContext;

import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class AlpharacksCoupon {

	private Pattern pattern = Pattern.compile("<strong>Price: (.*?)</strong>");
	private HttpHost host = new HttpHost("www.alpharacks.com", 443, "https");
	private HttpClient client = HttpClients.custom().setSSLSocketFactory(createSSLSockectFactory()).build();

	private SSLConnectionSocketFactory createSSLSockectFactory() {
		try {
			SSLContext context = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
				@Override
				public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					return true;
				}
			}).build();
			return new SSLConnectionSocketFactory(context, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			throw new RuntimeException(e);
		}
	}

	public Result fetchAff180(int pid) {
		try {
			HttpUriRequest request = RequestBuilder.get().setUri("/myrack/aff.php").addParameter("aff", "180")
					.addParameter("pid", Integer.toString(pid)).build();
			HttpResponse resp = client.execute(host, request);
			String content = EntityUtils.toString(resp.getEntity(), Consts.UTF_8);
			Matcher matcher = pattern.matcher(content);
			if (matcher.find()) {
				return new Result(matcher.group(1), "https://www.alpharacks.com/myrack/aff.php?aff=180&pid=" + pid);
			}
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Result fetchAdd(int pid) {
		try {
			HttpUriRequest request = RequestBuilder.get().setUri("/myrack/cart.php").addParameter("a", "add")
					.addParameter("pid", Integer.toString(pid)).build();
			HttpResponse resp = client.execute(host, request);
			String content = EntityUtils.toString(resp.getEntity(), Consts.UTF_8);
			Matcher matcher = pattern.matcher(content);
			if (matcher.find()) {
				return new Result(matcher.group(1), "https://www.alpharacks.com/myrack/cart.php?a=add&pid=" + pid);
			}
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) throws Exception {
		AlpharacksCoupon coupon = new AlpharacksCoupon();
		PrintStream out = new PrintStream("C:/Users/Dony/AlpharacksCoupon.txt");
		ForkJoinPool pool = new ForkJoinPool(10);

		pool.submit(() -> IntStream.range(0, 500).parallel().mapToObj(coupon::fetchAff180).filter((obj) -> obj != null)
				.peek(System.out::println).collect(Collectors.toList()).stream().forEach(out::println)).get();

		pool.submit(() -> IntStream.range(0, 300).parallel().mapToObj(coupon::fetchAdd).filter((obj) -> obj != null)
				.peek(System.out::println).collect(Collectors.toList()).stream().forEach(out::println)).get();

		out.close();
	}

	public static class Result {
		private String price;
		private String url;

		public Result(String price, String url) {
			this.price = price;
			this.url = url;
		}

		@Override
		public String toString() {
			return "[" + price + "] " + url;
		}
	}
}
