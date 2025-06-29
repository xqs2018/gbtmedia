package cn.gbtmedia.common.extra;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * @author xqs
 */
@Component
public class LocalFileHandler extends ResourceHttpRequestHandler {

    public final static String FILE_PATH = "localFilePath";

    @Override
    protected Resource getResource(HttpServletRequest request){
        String filePath = (String) request.getAttribute(FILE_PATH);
        return new FileSystemResource(filePath);
    }
}
