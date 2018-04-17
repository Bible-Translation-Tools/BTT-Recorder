package org.wycliffeassociates.translationrecorder.recordingapp;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.wycliffeassociates.translationrecorder.data.model.Project;
import org.wycliffeassociates.translationrecorder.data.model.Anthology;
import org.wycliffeassociates.translationrecorder.data.model.Book;
import org.wycliffeassociates.translationrecorder.data.model.Language;
import org.wycliffeassociates.translationrecorder.chunkplugin.Mode;
import org.wycliffeassociates.translationrecorder.data.model.Version;

import static org.mockito.Mockito.mock;

/**
 * Created by sarabiaj on 9/26/2017.
 */


public class ChapterCardTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    public Project mockProject;

    @Before
    public void setUp(){
        Anthology mockAnth = mock(Anthology.class);
        Book mockBook = mock(Book.class);
        Version mockVersion = mock(Version.class);
        Language mockLang = mock(Language.class);
        Mode mockMode = mock(Mode.class);
        //Project

    }

    @Test
    public void testChapterCardText() {
        mockProject = mock(Project.class);
        System.out.println("yehhh");
    }
}
