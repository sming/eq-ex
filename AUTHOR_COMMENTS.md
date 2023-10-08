# Feedback and comments on this implementation

## Thread safety

* I utilized ConcurrentHashMap to store the cart's contents since it performs well in both multithreaded and single-threaded environments and is trivial to code up.
* I did not make methods such as getProductCounts() synchronized as the properties of the ConcurrentHashMap are such that such operations e.g. entrySet() are thread-safe. They might not give the very latest contents, but for the purposes of a shopping cart, that is totally OK IMO.

## Other

* I feel that addProduct()'s usage of Map.compute() is really nice and succinct. First time I've used it. It avoids a lot of toll, in a similar vein to Python's defaultdict.
* The error philosophy/protocol adopted is to basically allow any exceptions thrown to propagate, rather than adding a ton of exception-handling code, which at the end of the day would probably want to throw exceptions anyway. It all just depends on what contract you declare for your class' API.

## Documentation

* Please find Javadoc at [app/build/docs/javadoc
app/build/docs](./app/build/docs/javadoc/)
