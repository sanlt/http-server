package httpserver.core;

import httpserver.core.protocol.HttpRequest;
import httpserver.core.protocol.HttpResponse;
import httpserver.core.protocol.HttpStatus;
import httpserver.framework.FrontController;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * The class RequestWorker is responsible for the processing of HTTP requests.
 */
public class RequestWorker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(RequestWorker.class.getName());

    private Socket socket;

    public RequestWorker(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (InputStream inputStream = socket.getInputStream(); OutputStream outputStream = socket.getOutputStream()) {
            HttpRequest request = new HttpRequest(inputStream);
            HttpResponse response = new HttpResponse(outputStream);
            request.parse();
            processRequest(request, response);
            response.send();
        } catch (IOException ex) {
            LOGGER.severe(ex.toString());
        } finally {
            try {
                socket.close();
                LOGGER.info("Connection to " + socket.getInetAddress() + " closed");
            } catch (IOException ex) {
            }
        }
    }

    private void processRequest(HttpRequest request, HttpResponse response) throws IOException {

        boolean success = FileDeliverer.deliverFile(request.getPath(), response);

        if (!success) {
            try {
                success = FrontController.processRequest(request, response);
            } catch (RuntimeException ex) {
                LOGGER.severe(ex.toString());
                LOGGER.severe(Arrays.toString(ex.getStackTrace()));
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
                response.writeBody("<html><body>500 Internal Server Error</body></html>");
                return;
            }
        }

        if (!success) {
            response.setStatus(HttpStatus.NOT_FOUND);
            response.writeBody("<html><body>404 Not Found</body></html>");
        }


    }
}
