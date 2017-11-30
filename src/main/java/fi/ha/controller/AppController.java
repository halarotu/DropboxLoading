package fi.ha.controller;

import com.dropbox.core.v2.files.FileMetadata;
import fi.ha.service.LoadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AppController {

    @Autowired
    LoadingService loadingService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String getImage(@RequestParam(name = "previous", required = false) Boolean previous, Model model) {

        FileMetadata fileMetadata;
        if (previous != null && previous) {
            fileMetadata = loadingService.getFile(true);
        } else if (loadingService.isAutoUpdateStopped()) {
            fileMetadata = loadingService.getCurrentMetadata();
        } else {
            fileMetadata = loadingService.getFile(false);
        }
        if (fileMetadata != null) {
            model.addAttribute("image", fileMetadata.getName());
            model.addAttribute("date", fileMetadata.getMediaInfo().getMetadataValue().getTimeTaken());
            model.addAttribute("rotate", loadingService.rotateImage(fileMetadata));
        }
        model.addAttribute("autoupdate", !loadingService.isAutoUpdateStopped());
        model.addAttribute("stopButtonText", loadingService.isAutoUpdateStopped() ? "Jatka" : "Pysäytä tähän");

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

    @RequestMapping(value = "/previous", method = RequestMethod.POST)
    public String getPrevious(RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("previous", true);
        return "redirect:/";
    }
}
