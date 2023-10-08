package com.equalexperts.shopping;

import com.equalexperts.shopping.exceptions.CartException;
import com.equalexperts.shopping.exceptions.ProductPriceException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The Cart class represents a shopping cart. One can add products, check how many of each product
 * one has, get the subtotal, get the tax payable, and get the total payable.
 * <p>
 * It is a thread-safe class.
 * <p>
 * To communicate problems/errors, this class will throw exceptions. For instance, the constructor
 * can throw if the config file is not found, and addProduct can throw if the specified product's
 * price could not be obtained, or if there was an error communicating with the pricing service.
 * This is a conscious decision and lets the user decide how to react, whilst *forcing them* to
 * decide how to react. This
 */
public class Cart {

  private static final String DEFAULT_TAX_RATE = "0.125";
  private static final String DEFAULT_PRODUCT_API_URL = "https://equalexperts.github.io/";
  private final double taxRate;
  private final String baseUrl;
  /**
   * productTotals is a map of product names to the total price of that product. Note that no
   * rounding is done here, it is only done when tax is applied.
   * <p>
   * Because it is a ConcurrentHashMap, operations upon it are thread-safe out of the box.
   */
  private final Map<String, Double> productTotals = new ConcurrentHashMap<>();
  /**
   * productCounts is a map of product names to the number of that product, and just like the
   * productTotals map, no rounding is done here.
   * <p>
   * Because it is a ConcurrentHashMap, operations upon it are thread-safe out of the box.
   */
  private final Map<String, Integer> productCounts = new ConcurrentHashMap<>();

  public Cart() throws IOException {
    var props = ConfigFileParser.getProperties();
    this.baseUrl = props.getProperty("product.api.url", DEFAULT_PRODUCT_API_URL);
    this.taxRate = Double.parseDouble(props.getProperty("product.tax_rate", Cart.DEFAULT_TAX_RATE));
  }

  public List<Entry<String, Double>> getProductTotals() {
    return productTotals.entrySet().stream().sorted(Map.Entry.comparingByKey())
        .collect(Collectors.toList());
  }

  public List<Entry<String, Integer>> getProductCounts() {
    return productCounts.entrySet().stream().sorted(Map.Entry.comparingByKey())
        .collect(Collectors.toList());
  }

  public Double getProductTotal(String productName) {
    return productTotals.getOrDefault(productName, 0.0);
  }

  public Integer getProductCount(String productName) {
    return productCounts.getOrDefault(productName, 0);
  }

  public Double getSubtotal() {
    return productTotals.values().stream().reduce(0.0, Double::sum);
  }

  public Double getTaxPayable() {
    return roundUpPrice(getSubtotal() * taxRate);
  }

  public Double getTotalPayable() {
    return roundUpPrice(getSubtotal() * (1.0 + taxRate));
  }

  public Cart addProduct(String productName, int quantity)
      throws IOException, InterruptedException, JSONException {
    if (quantity <= 0) {
      throw new CartException("Quantity must be greater than 0");
    }

    HttpResponse<String> response = getProductPrice(productName);

    if (response.statusCode() == 200) {
      var body = response.body();
      var obj = new JSONObject(body);
      double price = obj.getDouble("price");
      productTotals.compute(productName,
          (k, v) -> (v == null) ? quantity * price : v + quantity * price);
      productCounts.compute(productName,
          (k, v) -> (v == null) ? quantity : v + quantity);
      return this;
    } else {
      throw new ProductPriceException("Could not add '" + productName
          + "' to cart. The following HTTP Code was received when fetching the product's price: "
          + response.statusCode());
    }
  }

  private HttpResponse<String> getProductPrice(String productName)
      throws IOException, InterruptedException {
    var url = String.format("%s/%s.json", this.baseUrl, productName);
    var request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()
        .build();

    HttpResponse<String> response = HttpClient.newBuilder()
        .build()
        .send(request, BodyHandlers.ofString());
    return response;
  }

  private static double roundUpPrice(double price) {
    var bd = new BigDecimal(price).setScale(2, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }
}
