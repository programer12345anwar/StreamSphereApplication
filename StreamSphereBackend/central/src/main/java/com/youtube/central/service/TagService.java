package com.youtube.central.service;

import com.youtube.central.models.Tag;
import com.youtube.central.repository.TagRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TagService {

    @Autowired
    TagRepo tagRepo;


    public Tag getTagByName(String name){
        return tagRepo.findByName(name);
    }

    // To get tag objects from tag table and return the list of tag objects
    public List<Tag> getAllTagsFromSystem(List<String> tags){
        List<Tag> dbTagList = new ArrayList<>();
        for(int i = 0; i < tags.size(); i++){
            String tag = tags.get(i);
            // now we need to check is this tag present in system or not
            Tag tagObj = this.getTagByName(tag);
            if(tagObj == null){
                // tag with this particular name is not prsent in system
                // We need to create tag in system
                Tag newTag = new Tag();
                newTag.setName(tag);
                tagRepo.save(newTag);
                dbTagList.add(newTag);
            }else {
                dbTagList.add(tagObj);
            }
        }

        return dbTagList;
    }
}