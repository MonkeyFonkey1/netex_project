package com.netex.addressbook.image;

import com.netex.addressbook.PostgresTestConfiguration;
import com.netex.addressbook.activity.ContactActivityPublisher;
import com.netex.addressbook.auth.AppUser;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.outbox.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@TestPropertySource(properties = "app.picture-directory=target/test-pictures")
class ContactPictureApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ContactPictureService pictures;
    @MockitoBean ContactActivityPublisher activity;

    @Test
    void failedEventSaveRollsBackPictureChangeAndRemovesTheNewFile() throws Exception {
        TestAccount author = account();
        AppUser user = new AppUser(author.id(), "author@example.com", "test-hash", "USER");
        long id = contact(author.id());
        pictures.replace(id, user, picture(png(Color.BLUE)));
        String original = picturePath(id);
        long filesBefore;
        try (var files = Files.list(Path.of("target/test-pictures"))) {
            filesBefore = files.count();
        }

        doThrow(new IllegalStateException("Outbox unavailable")).when(activity).updated(id, author.id());
        assertThatThrownBy(() -> pictures.replace(id, user, picture(png(Color.RED))))
                .isInstanceOf(IllegalStateException.class);

        assertThat(picturePath(id)).isEqualTo(original);
        assertThat(Files.exists(file(original))).isTrue();
        try (var files = Files.list(Path.of("target/test-pictures"))) {
            assertThat(files.count()).isEqualTo(filesBefore);
        }
    }

    @Test
    void authorCanUploadReplaceAndRemoveAPictureWhileEveryoneCanReadIt() throws Exception {
        TestAccount author = account();
        long id = contact(author.id());
        byte[] first = png(Color.BLUE);
        byte[] second = png(Color.RED);

        mockMvc.perform(get("/api/contacts/{id}/picture", id)).andExpect(status().isNotFound());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/contacts/{id}/picture", id)
                        .file(picture(first)).with(csrf()).session(author.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pictureUrl").isNotEmpty())
                .andExpect(jsonPath("$.canManage").value(true));
        String firstName = picturePath(id);
        assertThat(firstName).endsWith(".png");
        assertThat(Files.readAllBytes(file(firstName))).isEqualTo(first);

        mockMvc.perform(get("/api/contacts/{id}/picture", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(first));
        mockMvc.perform(get("/api/contacts/{id}", id))
                .andExpect(jsonPath("$.pictureUrl").isNotEmpty())
                .andExpect(jsonPath("$.picturePath").doesNotExist());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/contacts/{id}/picture", id)
                        .file(picture(second)).with(csrf()).session(author.session()))
                .andExpect(status().isOk());
        String secondName = picturePath(id);
        assertThat(secondName).isNotEqualTo(firstName);
        assertThat(Files.exists(file(firstName))).isFalse();
        assertThat(new PictureStorage("target/test-pictures").read(secondName)).isEqualTo(second);

        mockMvc.perform(delete("/api/contacts/{id}/picture", id).with(csrf()).session(author.session()))
                .andExpect(status().isNoContent());
        assertThat(picturePath(id)).isNull();
        assertThat(Files.exists(file(secondName))).isFalse();
        mockMvc.perform(get("/api/contacts/{id}/picture", id)).andExpect(status().isNotFound());
        verify(activity, times(3)).updated(id, author.id());
    }

    @Test
    void regularUsersCanChangeOnlyTheirOwnPictureAndDeletingTheContactRemovesItsFile() throws Exception {
        TestAccount author = account();
        TestAccount other = account();
        long id = contact(author.id());
        byte[] bytes = png(Color.GREEN);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/contacts/{id}/picture", id).file(picture(bytes)).with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/contacts/{id}/picture", id)
                        .file(picture(bytes)).with(csrf()).session(other.session()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/contacts/{id}/picture", id).with(csrf()).session(other.session()))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/contacts/{id}/picture", id)
                        .file(picture(bytes)).with(csrf()).session(author.session()))
                .andExpect(status().isOk());
        String filename = picturePath(id);
        mockMvc.perform(delete("/api/contacts/{id}", id).with(csrf()).session(author.session()))
                .andExpect(status().isNoContent());
        verify(activity).updated(id, author.id());
        verify(activity).deleted(id, author.id());
        assertThat(Files.exists(file(filename))).isFalse();
    }

    @Test
    void adminCanReplaceAndRemoveAnotherUsersPicture() throws Exception {
        TestAccount author = account();
        TestAccount admin = account("ADMIN");
        long id = contact(author.id());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/contacts/{id}/picture", id)
                        .file(picture(png(Color.ORANGE))).with(csrf()).session(admin.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canManage").value(true));
        String filename = picturePath(id);
        assertThat(Files.exists(file(filename))).isTrue();

        mockMvc.perform(delete("/api/contacts/{id}/picture", id).with(csrf()).session(admin.session()))
                .andExpect(status().isNoContent());
        assertThat(picturePath(id)).isNull();
        assertThat(Files.exists(file(filename))).isFalse();
        verify(activity, times(2)).updated(id, admin.id());
    }

    @Test
    void rejectsNonImagesAndPicturesLargerThanFiveMegabytes() throws Exception {
        TestAccount author = account();
        long id = contact(author.id());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/contacts/{id}/picture", id)
                        .file(new MockMultipartFile("picture", "fake.png", "image/png", "not a picture".getBytes()))
                        .with(csrf())
                        .session(author.session()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/contacts/{id}/picture", id)
                        .file(picture(Arrays.copyOf(png(Color.BLUE), 40))).with(csrf()).session(author.session()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart(HttpMethod.PUT, "/api/contacts/{id}/picture", id)
                        .file(new MockMultipartFile("picture", "huge.png", "image/png", new byte[5 * 1024 * 1024 + 1]))
                        .with(csrf())
                        .session(author.session()))
                .andExpect(status().isPayloadTooLarge());
        assertThat(picturePath(id)).isNull();
        verifyNoInteractions(activity);
    }

    private TestAccount account() throws Exception {
        return account("USER");
    }

    private TestAccount account(String role) throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        long id = jdbc.queryForObject("INSERT INTO users (email, password_hash, role) VALUES (?, ?, ?) RETURNING id",
                Long.class, email, passwordEncoder.encode("strong-password"), role);
        MvcResult login = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .param("email", email).param("password", "strong-password"))
                .andExpect(status().isNoContent()).andReturn();
        return new TestAccount(id, (MockHttpSession) login.getRequest().getSession(false));
    }

    private long contact(long authorId) {
        return jdbc.queryForObject("""
                INSERT INTO contacts (name, address, created_by_user_id)
                VALUES ('Photo test', 'Test address', ?) RETURNING id
                """, Long.class, authorId);
    }

    private String picturePath(long id) {
        return jdbc.queryForObject("SELECT picture_path FROM contacts WHERE id = ?", String.class, id);
    }

    private Path file(String filename) {
        return Path.of("target/test-pictures", filename);
    }

    private MockMultipartFile picture(byte[] bytes) {
        return new MockMultipartFile("picture", "contact.png", "image/png", bytes);
    }

    private byte[] png(Color color) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, color.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, "png", output)).isTrue();
        return output.toByteArray();
    }

    private record TestAccount(long id, MockHttpSession session) {
    }
}
