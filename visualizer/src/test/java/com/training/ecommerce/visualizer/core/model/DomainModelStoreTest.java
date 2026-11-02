package com.training.ecommerce.visualizer.core.model;

import com.training.ecommerce.visualizer.core.model.DomainModel.ClassDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.ContextDef;
import com.training.ecommerce.visualizer.core.model.DomainModel.RelationDef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.training.ecommerce.visualizer.core.model.FakeDomain.SPEC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainModelStoreTest {

    @TempDir
    Path dir;

    private DomainModelStore store(Path path) {
        return new DomainModelStore(SPEC, () -> List.of(), path);
    }

    private static DomainModel valid() {
        return new DomainModel(1, List.of(new ContextDef("sales", "Bán hàng", "--hub")),
                List.of(new ClassDef("Order", "sales", "aggregate", false, "manual"),
                        new ClassDef("Line", "sales", "entity", false, null)),
                List.of(new RelationDef("Order", "Line", "field")));
    }

    @Test
    void khongCoFileThiTraGoiY() {
        ModelSnapshot snap = store(dir.resolve("domain-model.json")).current();
        assertThat(snap.origin()).isEqualTo("suggestion");
        assertThat(snap.model()).isEqualTo(DomainModelSuggester.suggest(SPEC));
    }

    @Test
    void khongCoFileThiDocClasspathTruocGoiY() {
        ModelSnapshot snap = new DomainModelStore(SPEC, () -> List.of(), dir.resolve("domain-model.json"),
                "/model/fallback-model.json").current();
        assertThat(snap.origin()).isEqualTo("classpath");
        assertThat(snap.model().classes()).extracting(ClassDef::cls).containsExactly("Order");
    }

    @Test
    void luuRoiCurrentTraDungMoHinhOriginFile() {
        DomainModelStore store = store(dir.resolve("sub/dir/domain-model.json"));
        store.current(); // nạp cache là gợi ý trước
        ModelSnapshot saved = store.save(valid());

        assertThat(saved.origin()).isEqualTo("file");
        assertThat(store.current().origin()).isEqualTo("file");
        assertThat(store.current().model()).isEqualTo(valid());
        // Instance mới đọc lại từ file
        ModelSnapshot reread = store(dir.resolve("sub/dir/domain-model.json")).current();
        assertThat(reread.origin()).isEqualTo("file");
        assertThat(reread.model()).isEqualTo(valid());
    }

    @Test
    void fileLaJsonPrettyVaKhongDeLaiFileTam() throws IOException {
        Path file = dir.resolve("domain-model.json");
        store(file).save(valid());
        String text = Files.readString(file);
        assertThat(text).contains("\n").contains("  \"version\" : 1").contains("\"cls\" : \"Order\"");
        try (var files = Files.list(dir)) {
            assertThat(files.toList()).containsExactly(file);
        }
    }

    @Test
    void moHinhKhongHopLeNemLoiVaKhongDungFile() throws IOException {
        Path file = dir.resolve("domain-model.json");
        Files.writeString(file, "{\"version\":1,\"contexts\":[],\"classes\":[],\"relations\":[]}");
        String before = Files.readString(file);
        DomainModelStore store = store(file);
        DomainModel bad = new DomainModel(null, List.of(), List.of(new ClassDef("Ma", null, "value", false, null)),
                List.of());

        assertThatThrownBy(() -> store.save(bad)).isInstanceOfSatisfying(InvalidModelException.class,
                e -> assertThat(e.errors()).hasSizeGreaterThanOrEqualTo(2));
        assertThat(Files.readString(file)).isEqualTo(before);
        assertThat(store.current().origin()).isEqualTo("file");
    }

    @Test
    void khongGhiDuocThiNemModelNotWritable() throws IOException {
        Path regular = dir.resolve("la-file");
        Files.writeString(regular, "x");
        DomainModelStore store = store(regular.resolve("domain-model.json"));
        assertThatThrownBy(() -> store.save(valid())).isInstanceOf(ModelNotWritableException.class)
                .hasMessageContaining("Không ghi được");
    }

    @Test
    void validateThamSo() {
        assertThatThrownBy(() -> new DomainModelStore(null, () -> List.of(), dir))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DomainModelStore(SPEC, null, dir))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DomainModelStore(SPEC, () -> List.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store(dir.resolve("x.json")).save(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
