import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;

public class reCAPTCHA_v2 {

    private static class Code {
        public String uuid;
        public String token;

        public Code(String uuid, String token) {
            this.uuid = uuid;
            this.token = token;
        }

        @Override
        public String toString() {
            return "Code{" +
                    "uuid='" + uuid + '\'' +
                    ", token='" + token + '\'' +
                    '}';
        }
    }

    private static final String context = "/reCAPTCHA_v2";
    private static String siteKey="";
    private static final List<Code> CODELIST = new ArrayList<Code>();

    public static void main(String[] args) throws Exception {
        if (args == null || args.length != 2) {
            System.err.println("Need 2 args: port and siteKey!");
            System.err.println("Like this: java reCAPTCHA_v2 8888 6LfMLX8Uff00bb_QQ1109691620QQ_xmK4H");
            System.exit(-1);
        }
        String portStr = args[0];
        int port = 8888;
        if (portStr != null && portStr.trim().length() > 0) {
            try {
                port = Integer.valueOf(portStr);
            } catch (Exception e) {
                System.out.println("port must be number");
            }
        }
        siteKey = args[1];
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext(context, new reCAPTCHA_v2Handler());
        server.start();
        System.out.println("Server is Running at http://localhost:" + port + context);
    }

    static class reCAPTCHA_v2Handler implements HttpHandler {
        public void handle(final HttpExchange exchange) throws IOException {
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    try {
//                        System.out.println("receive");
//                        System.out.println(CODELIST);
                        String reqMethod = exchange.getRequestMethod();

                        Map<String, Object> postDataMap = new HashMap<String, Object>();
                        InputStream requestBody = exchange.getRequestBody();
                        if (requestBody != null) {
                            StringBuilder sb = new StringBuilder();
                            byte[] b = new byte[4096];
                            int len;
                            while ((len = requestBody.read(b)) != -1) {
                                sb.append(new String(b, 0, len));
                            }
                            String s = sb.toString();
//                            System.out.println("req body = " + s);
                            String[] postDatas = s.split("&");
                            if (postDatas.length != 0) {
                                for (int i = 0; i < postDatas.length; i++) {
                                    String kv = postDatas[i];
                                    String[] kvs = kv.split("=");
                                    if (kvs.length == 2) {
                                        postDataMap.put(kvs[0], kvs[1]);
                                    }
                                }
                            }

                        }
                        URI requestURI = exchange.getRequestURI();
                        System.out.println(requestURI);
                        String requestURIQuery = requestURI.getQuery();
//                        System.out.println(requestURIQuery);

                        Map<String, Object> queryMap = new HashMap<String, Object>();
                        if (requestURIQuery != null) {
                            String[] queries = requestURIQuery.split("&");
                            if (queries.length != 0) {
                                for (int i = 0; i < queries.length; i++) {
                                    String kv = queries[i];
                                    String[] kvs = kv.split("=");
                                    if (kvs.length == 2) {
                                        queryMap.put(kvs[0], kvs[1]);
                                    }
                                }
                            }
                        }
//                        System.out.println("queryMap = " + queryMap);

                        Headers responseHeaders = exchange.getResponseHeaders();
                        exchange.sendResponseHeaders(200, 0);
                        OutputStream os = exchange.getResponseBody();
                        OutputStreamWriter writer = new OutputStreamWriter(os, "utf-8");

                        if ("GET".equalsIgnoreCase(reqMethod)) {
                            responseHeaders.set("Content-Type", "text/html;charset=utf-8");
                            if (queryMap.containsKey("method")) {
                                String method = (String) queryMap.get("method");
                                if ("getToken".equalsIgnoreCase(method)) {
                                    if (CODELIST.size() > 0) {
                                        if (queryMap.containsKey("uuid")) {
                                            for (int i = 0; i < CODELIST.size(); i++) {
                                                Code code = CODELIST.get(i);
                                                if (code.uuid.equalsIgnoreCase((String) queryMap.get("uuid"))) {
                                                    writer.write(code.token);
                                                    CODELIST.remove(code);
                                                    break;
                                                }
                                            }
                                        } else {
                                            Code code = CODELIST.get(0);
                                            writer.write(code.token);
                                            CODELIST.remove(code);
                                        }
                                    } else {
                                        writer.write("");
                                    }
                                }
                            } else {
                                writer.write("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'><meta name='renderer' content='webkit|ie-comp|ie-stand'><meta http-equiv='X-UA-Compatible' content='IE=edge,chrome=1'><meta name='viewport' content='width=device-width,initial-scale=1,minimum-scale=1.0,maximum-scale=1.0,user-scalable=no' /><meta http-equiv='Cache-Control' content='no-siteapp' /><title>reCAPTCHA_v2</title><script src='https://www.recaptcha.net/recaptcha/api.js'></script> </head><body><div class='g-recaptcha' data-sitekey='"+siteKey+"' data-callback='recaptchaCallback'></div> <input type='button' value='重新生成' id='btn_reset' hidden=true/><br/><label id='lbl_result'></label><script type='text/javascript'>var btn_reset=document.getElementById('btn_reset');var lbl_result=document.getElementById('lbl_result');btn_reset.onclick=function(){grecaptcha.reset();btn_reset.hidden=true;lbl_result.innerHTML=''};function recaptchaCallback(b){var c=grecaptcha.getResponse();console.log(c);console.log(b);btn_reset.hidden=false;if(b===c){var a=new Ajax();a.send({method:'POST',url:'?method=upload',resType:'json',data:{token:c},success:function(d){console.log(d);lbl_result.innerHTML=d['msg']+'\\n'+d['uuid']},error:function(d){console.error(d);lbl_result.innerHTML=d}})}}; </script><script type='text/javascript'>(function(){function a(){var b=window.XMLHttpRequest?new XMLHttpRequest():new ActiveXObject('Microsoft.XMLHttp');if(!b){alert('您的浏览器不支持XMLHttpRequest对象');return}this.xhr=b}a.prototype.send=function(d){if(typeof d!='object'||d===null){alert('请正确配置参数');return}d.method=d.method?d.method.toUpperCase():'GET';d.url=d.url||'';d.async=d.async==false?false:true;d.data=d.data||null;d.resType=d.resType||'text';d.success=d.success||function(f){console.log(f)};d.error=d.error||function(f){console.log('error: status='+f)};var e=['t='+(new Date()).valueOf()];if(d.data){for(var c in d.data){e.push(c+'='+d.data[c])}}var b=e.join('&');this.xhr.onreadystatechange=function(){if(this.xhr.readyState==4){if(this.xhr.status==200){if(d.resType=='text'){d.success(this.xhr.responseText)}else{if(d.resType=='xml'){d.success(this.xhr.responseXML)}else{if(d.resType=='json'){try{var g=new Function('return '+this.xhr.responseText);d.success(g())}catch(f){d.error('Not a json')}}}}}else{d.error(this.xhr.status)}}}.bind(this);if(d.method=='GET'){this.xhr.open('GET',d.url+'?'+b,d.async)}else{if(d.method=='POST'){this.xhr.open('POST',d.url,d.async);this.xhr.setRequestHeader('Content-Type','application/x-www-form-urlencoded;charset=utf-8')}}this.xhr.send(b)};a.prototype.abort=function(){this.xhr.abort()};window.Ajax=a})();</script></body></html>");
                            }
                        } else {
                            if (queryMap.containsKey("method")) {
                                String method = (String) queryMap.get("method");
                                if ("upload".equalsIgnoreCase(method)) {
                                    // upload token
                                    if (postDataMap.containsKey("token")) {
                                        String code = (String) postDataMap.get("token");
                                        String uuid = UUID.randomUUID().toString();
                                        CODELIST.add(new Code(uuid, code));
                                        String result = "{\"msg\": \"成功\", \"uuid\": \"" + uuid + "\"}";
                                        writer.write(result);
                                    }
                                }
                            } else {
                                writer.write("{\"msg\":\"非法请求\"}");
                            }
                        }
                        writer.close();
                        os.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }.start();
        }
    }
}
