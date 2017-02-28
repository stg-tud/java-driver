package tech.sourced.babelfish;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.commons.io.IOUtils;

import java.io.*;

public class Main {

    public static void main(String args[]) {

        BufferedInputStream in = new BufferedInputStream(System.in);
        BufferedOutputStream out = new BufferedOutputStream(System.out);

        while (true) {
            try {
                process(in, out);
            } catch (IOException e) {
                //This exception only occurs when you can't write in System.out
                System.err.println("Can't write in the output given " + e.toString());
                System.exit(1);
            }
        }

    }

    static private void process(BufferedInputStream in, BufferedOutputStream out) throws IOException {

        final EclipseParser parser = new EclipseParser();
        final RequestResponseMapper mapperGen = new RequestResponseMapper(true);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final RequestResponseMapper.ResponseMapper responseMapper = mapperGen.getResponseMapper(baos);

        while (true) {
            String inStr;
            final DriverResponse response = new DriverResponse("1.0.0", "Java", "8");
            response.setMapper(responseMapper);
            try {
                inStr = IOUtils.toString(in, "UTF-8");
                DriverRequest request;
                try{
                    request = DriverRequest.unpack(inStr);
                }catch(JsonMappingException e){
                    exceptionPrinter(e,"Error reading the petition: ",baos,out,response);
                    return;
                }
                response.makeResponse(parser, request.content);
                response.pack();
                baos.flush();
                baos.reset();
                out.write(baos.toByteArray());
                out.flush();
            }catch(JsonMappingException e){
                exceptionPrinter(e,"Error serializing the AST to JSON: ",baos,out,response);
                return;
            }catch (IOException e) {
                exceptionPrinter(e,"A problem occurred while processing the petition: ",baos,out,response);
                return;
            }

        }
    }

    static private void exceptionPrinter(Exception e,String errorString,ByteArrayOutputStream baos,BufferedOutputStream out,DriverResponse response)throws IOException{
        response.cu = null;
        response.errors.add(e.getClass().getCanonicalName());
        response.errors.add(errorString + e.getMessage());
        response.status = "fatal";
        response.pack();
        out.write(baos.toByteArray());
        out.flush();
    }
}
