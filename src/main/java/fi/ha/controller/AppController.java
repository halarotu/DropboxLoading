package fi.ha.controller;

import com.dropbox.core.v2.files.FileMetadata;
import fi.ha.service.LoadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class AppController {

    @Autowired
    LoadingService loadingService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String getImage(Model model) {

        FileMetadata fileMetadata;
        if (loadingService.isAutoUpdateStopped()) {
            fileMetadata = loadingService.getCurrentMetadata();
        } else {
            fileMetadata = loadingService.getFile();
        }
        model.addAttribute("image", fileMetadata.getName());
        model.addAttribute("date", fileMetadata.getMediaInfo().getMetadataValue().getTimeTaken());

        boolean rotate = false;
        if (fileMetadata.getMediaInfo().getMetadataValue().getDimensions().getHeight() >
                fileMetadata.getMediaInfo().getMetadataValue().getDimensions().getWidth()) {
            rotate = true;
        }
        model.addAttribute("rotate", rotate);
        model.addAttribute("autoupdate", !loadingService.isAutoUpdateStopped());
        String buttonText = loadingService.isAutoUpdateStopped() ? "Jatka" : "Pysäytä tähän";
        model.addAttribute("buttonText", buttonText);

        return "main";
    }

    @RequestMapping(value = "/startStop", method = RequestMethod.POST)
    public String startStop() {
        if (loadingService.isAutoUpdateStopped()) {
            loadingService.setAutoUpdateStopped(false);
        } else {
            loadingService.setAutoUpdateStopped(true);
        }

        return "redirect:/";
    }
}
