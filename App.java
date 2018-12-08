package com.microandroid;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * App
 * @author lewjun 
 */
public class App {
    private static final String CONTEXT_ROOT = "/";
    private static final String CONTEXT = "/app";
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

    public static void main(String[] args) {
        try {
            int port = 8888;
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            AppHandler httpHandler = new AppHandler();
            httpServer.createContext(CONTEXT_ROOT, httpHandler);
            httpServer.createContext(CONTEXT, httpHandler);
            httpServer.createContext("/media/get", new MediaGetHandler());
            httpServer.createContext("/media/up", new MediaUpHandler());
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();
            System.out.println(String.format("Server is running at http://localhost:%d%s", port, CONTEXT));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class MediaUpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            InputStream input = exchange.getRequestBody();
            DataInputStream dis = new DataInputStream(input);
            String line;
            while ((line = dis.readLine()) != null) {
                System.out.println(line);

                // TODO Unimplemented
            }

            exchange.sendResponseHeaders(200, 0);
            OutputStream out = exchange.getResponseBody();
            out.write("ok".getBytes());
            out.close();
        }
    }

    static class MediaGetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            InputStream input = exchange.getRequestBody();
            OutputStream out = exchange.getResponseBody();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
//            PrintWriter writer = new PrintWriter(out);


            File file = new File("./pom.xml");
            System.out.println(file.getAbsolutePath());
            InputStream fileIn = new FileInputStream(file);
            int available = fileIn.available();
            byte[] buffer = new byte[available];
            int read = fileIn.read(buffer);
            System.out.println(read);
            exchange.sendResponseHeaders(200, read);
            Headers headers = exchange.getResponseHeaders();
            headers.add("Accept-Ranges", "bytes");
            headers.add("Content-type", "application/octet-stream");
            headers.add("Content-Disposition", "attachment;filename=pom.xml");
            headers.add("Content-Length", String.valueOf(read));
            out.write(buffer);
            out.close();
            fileIn.close();
        }
    }

    static class AppHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            /*new Thread(){
                @Override
                public void run() {
                    super.run();

                    // TODO make this to support multi thread
                }
            }.start();*/
            URI requestURI = exchange.getRequestURI();
            String path = requestURI.getPath();
            System.out.println(path);
            String query = requestURI.getQuery();
            InetSocketAddress remoteAddress = exchange.getRemoteAddress();
            System.out.println(remoteAddress.getHostString());

            String reqMethod = exchange.getRequestMethod();
            Headers requestHeaders = exchange.getRequestHeaders();
            Map<String, Object> reqQueryParams = parseQuery(query);
            System.out.println(reqQueryParams);
            Map<String, Object> reqBodyParams = parseQuery(inputToString(exchange.getRequestBody()));
            System.out.println(reqBodyParams);

            Map<String, Object> reqParams = mapMerge(reqQueryParams, reqBodyParams);

            // if path is root
            if (CONTEXT_ROOT.equalsIgnoreCase(path) || CONTEXT.equalsIgnoreCase(path)) {
                if (!reqQueryParams.containsKey("method")) {
                    handleRoot(exchange);
                } else {
                    String method = (String) reqQueryParams.get("method");
                    if ("signin".equalsIgnoreCase(method)) {
                        doSignin(exchange, reqParams);
                    } else if ("signup".equalsIgnoreCase(method)) {

                    } else {
                        write(exchange, null, "lost method");
                    }
                }
            } else {
                write(exchange, null, "not support");
            }
        }

        void doSignin(HttpExchange exchange, Map<String, Object> reqParams) throws IOException {
            String resp = "{code:1,msg:\"成功\"}";
            Map<String, String> headerValues = new HashMap<String, String>();
            headerValues.put("Content-Type", "application/json; charset=utf-8");
            write(exchange, headerValues, resp);
        }

        void handleRoot(HttpExchange exchange) throws IOException {
            String resp = "获取token";
            Map<String, String> headerValues = new HashMap<String, String>();
            headerValues.put("Content-Type", "text/html; charset=utf-8");
            write(exchange, headerValues, resp);
        }

        void write(HttpExchange exchange, Map<String, String> headerValues, String resp) throws IOException {
            OutputStream out = exchange.getResponseBody();
//            OutputStreamWriter writer = new OutputStreamWriter(out, UTF_8);
            Headers headers = exchange.getResponseHeaders();
            if (headers != null && headerValues != null && headerValues.size() > 0) {
                for (Map.Entry<String, String> me : headerValues.entrySet()) {
                    headers.set(me.getKey(), me.getValue());
                }
            }
            exchange.sendResponseHeaders(200, new String(resp.getBytes(UTF_8), ISO_8859_1).length());
//            exchange.sendResponseHeaders(200, 0);
//            writer.write(resp);

            out.write(resp.getBytes(UTF_8));
//            closeQuietly(writer);
            closeQuietly(out);

        }

        void closeQuietly(Closeable closeable) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    static String inputToString(InputStream input) throws IOException {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        byte[] b = new byte[4096];
        int len;
        while ((len = input.read(b)) != -1) {
            sb.append(new String(b, 0, len));
        }
        return sb.toString();
    }


    static Map<String, Object> mapMerge(Map<String, ?> map1, Map<String, ?> map2) {
        Map<String, Object> map3 = new HashMap<>();
        if (map1 != null && map1.size() > 0) {
            for (Map.Entry<String, ?> me : map1.entrySet()) {
                map3.put(me.getKey(), me.getValue());
            }
        }
        if (map2 != null && map2.size() > 0) {
            for (Map.Entry<String, ?> me : map2.entrySet()) {
                map3.put(me.getKey(), me.getValue());
            }
        }

        return map3;
    }

    static Map<String, Object> parseQuery(String query) throws UnsupportedEncodingException {
        Map<String, Object> parameters = new HashMap<>();
        if (query != null) {
            String pairs[] = query.split("[&]");
            for (String pair : pairs) {
                String param[] = pair.split("[=]");
                String key = null;
                String value = null;
                if (param.length > 0) {
                    key = URLDecoder.decode(param[0],
                            System.getProperty("file.encoding"));
                }

                if (param.length > 1) {
                    value = URLDecoder.decode(param[1],
                            System.getProperty("file.encoding"));
                }

                if (parameters.containsKey(key)) {
                    Object obj = parameters.get(key);
                    if (obj instanceof List<?>) {
                        List<String> values = (List<String>) obj;
                        values.add(value);

                    } else if (obj instanceof String) {
                        List<String> values = new ArrayList<String>();
                        values.add((String) obj);
                        values.add(value);
                        parameters.put(key, values);
                    }
                } else {
                    parameters.put(key, value);
                }
            }
        }
        return parameters;
    }
}
