import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;

public class InventoryWeb {
    // The only change is on this line
    static final int LOW_STOCK_THRESHOLD = 100; 

    static List<String[]> inventory = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new HomeHandler());
        server.createContext("/add", new AddHandler());
        server.createContext("/list", new ListHandler());
        server.createContext("/sortQuantity", new SortQuantityHandler());
        server.createContext("/sortExpiry", new SortExpiryHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started at http://localhost:8080/");
    }

    // Page wrapper with common CSS & layout
    private static String pageWrapper(String bodyContent) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Inventory Tracker with Low Stock Alert</title>"
                + "<style>"
                + "body{margin:0;font-family:Arial,sans-serif;background:#f5f5f5;}"
                + ".navbar{background:#0b0b3b;color:white;padding:15px;display:flex;align-items:center;}"
                + ".menu{font-size:24px;cursor:pointer;margin-right:15px;}"
                + ".sidebar{display:none;position:absolute;top:50px;left:0;background:#0b0b3b;color:white;width:200px;padding:10px;}"
                + ".sidebar a{color:white;display:block;padding:8px;text-decoration:none;}"
                + ".sidebar a:hover{background:#333;}"
                + ".card{background:#e0e0e0;padding:20px;border-radius:15px;width:300px;margin:50px auto;box-shadow:0 4px 10px rgba(0,0,0,0.2);}"
                + "input,button{width:100%;padding:10px;margin:8px 0;border-radius:8px;border:none;}"
                + "input{background:#0b0b3b;color:white;}"
                + "button{background:#0b0b3b;color:white;font-weight:bold;cursor:pointer;}"
                + "button:hover{background:#1a1a5e;}"
                + "table{margin:20px auto;border-collapse:collapse;width:80%;}"
                + "th,td{border:1px solid #999;padding:10px;text-align:center;}"
                + ".lowStock{color:red;font-weight:bold;}"
                + ".alert{width:80%;margin:10px auto;padding:15px;background:#ff6b6b;color:white;font-weight:bold;text-align:center;border-radius:5px;}"
                + "</style>"
                + "<script>"
                + "function toggleMenu(){var s=document.getElementById('sidebar');"
                + "s.style.display=(s.style.display==='block'?'none':'block');}"
                + "</script></head><body>"
                + "<div class='navbar'><div class='menu' onclick='toggleMenu()'>☰</div>INVENTORY TRACKER WITH LOW STOCK ALERT</div>"
                + "<div class='sidebar' id='sidebar'>"
                + "<a href='/'>Add Items</a>"
                + "<a href='/list'>View Inventory</a>"
                + "<a href='/sortQuantity'>Sort by Quantity</a>"
                + "<a href='/sortExpiry'>Sort by Expiry Date</a>"
                + "</div>"
                + bodyContent + "</body></html>";
    }

    // Home Page: Add Items Form
    static class HomeHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String response = pageWrapper(
                    "<div class='card'>"
                            + "<form method='get' action='/add'>"
                            + "<label>Item ID:</label><input name='id' required>"
                            + "<label>Quantity:</label><input name='qty' required>"
                            + "<label>Expiry Date (yyyymmdd):</label><input name='exp' required>"
                            + "<button type='submit'>ADD ITEMS</button>"
                            + "</form></div>");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Add Items
    static class AddHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            if (params.containsKey("id") && params.containsKey("qty") && params.containsKey("exp")) {
                inventory.add(new String[]{params.get("id"), params.get("qty"), params.get("exp")});
            }
            String response = pageWrapper("<div class='card'><p>✅ Item Added Successfully!</p>"
                    + "<a href='/'>➕ Add More</a><br><a href='/list'>📦 View Inventory</a></div>");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // List Inventory with Low Stock Alert
    static class ListHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            List<String> lowItems = getLowStockItems();
            StringBuilder sb = new StringBuilder();
            if(!lowItems.isEmpty()){
                sb.append("<div class='alert'>Low Stock Alert: ").append(String.join(", ", lowItems)).append("</div>");
            }
            sb.append("<h2 style='text-align:center;'>Inventory List</h2><table><tr><th>Item ID</th><th>Quantity</th><th>Expiry</th><th>Status</th></tr>");
            for (String[] item : inventory) {
                int qty = Integer.parseInt(item[1]);
                sb.append("<tr><td>").append(item[0]).append("</td><td>").append(item[1]).append("</td><td>").append(item[2])
                  .append("</td><td>").append(getLowStockStatus(qty)).append("</td></tr>");
            }
            sb.append("</table>");
            String response = pageWrapper(sb.toString());
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Sort by Quantity
    static class SortQuantityHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            inventory.sort(Comparator.comparingInt(o -> Integer.parseInt(o[1])));
            List<String> lowItems = getLowStockItems();
            StringBuilder sb = new StringBuilder();
            if(!lowItems.isEmpty()){
                sb.append("<div class='alert'>Low Stock Alert: ").append(String.join(", ", lowItems)).append("</div>");
            }
            sb.append("<h2 style='text-align:center;'>Sorted by Quantity</h2><table><tr><th>Item ID</th><th>Quantity</th><th>Expiry</th><th>Status</th></tr>");
            for (String[] item : inventory) {
                int qty = Integer.parseInt(item[1]);
                sb.append("<tr><td>").append(item[0]).append("</td><td>").append(item[1]).append("</td><td>").append(item[2])
                  .append("</td><td>").append(getLowStockStatus(qty)).append("</td></tr>");
            }
            sb.append("</table>");
            String response = pageWrapper(sb.toString());
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Sort by Expiry Date
    static class SortExpiryHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            inventory.sort(Comparator.comparing(o -> o[2]));
            List<String> lowItems = getLowStockItems();
            StringBuilder sb = new StringBuilder();
            if(!lowItems.isEmpty()){
                sb.append("<div class='alert'>Low Stock Alert: ").append(String.join(", ", lowItems)).append("</div>");
            }
            sb.append("<h2 style='text-align:center;'>Sorted by Expiry Date</h2><table><tr><th>Item ID</th><th>Quantity</th><th>Expiry</th><th>Status</th></tr>");
            for (String[] item : inventory) {
                int qty = Integer.parseInt(item[1]);
                sb.append("<tr><td>").append(item[0]).append("</td><td>").append(item[1]).append("</td><td>").append(item[2])
                  .append("</td><td>").append(getLowStockStatus(qty)).append("</td></tr>");
            }
            sb.append("</table>");
            String response = pageWrapper(sb.toString());
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // Low Stock Helpers
    private static String getLowStockStatus(int qty) {
        return qty < LOW_STOCK_THRESHOLD ? "<span class='lowStock'>⚠ Low Stock</span>" : "OK";
    }

    private static List<String> getLowStockItems() {
        List<String> lowItems = new ArrayList<>();
        for (String[] item : inventory) {
            if (Integer.parseInt(item[1]) < LOW_STOCK_THRESHOLD) {
                lowItems.add(item[0]);
            }
        }
        return lowItems;
    }

    // Query Parser
    private static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) result.put(pair[0], pair[1]);
        }
        return result;
    }
}