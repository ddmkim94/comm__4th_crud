package com.ll.exam;

import com.ll.exam.article.dto.ArticleDto;
import com.ll.exam.article.service.ArticleService;
import com.ll.exam.mymap.MyMap;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ArticleServiceTest {

    private final MyMap myMap;
    private final ArticleService articleService;
    private static final int TEST_DATE_SIZE = 100; // 테스트 케이스 개수! (찾아서 바꿀 필요가 없어짐!!)

    public ArticleServiceTest() {
        myMap = Container.getObj(MyMap.class);
        articleService = Container.getObj(ArticleService.class);
    }

    @BeforeAll
    public void beforeAll() {
        // 모든 DB 처리시에, 처리되는 SQL을 콘솔에 출력
        myMap.setDevMode(true);
    }

    // 각 테스트마다 실행되는 메서드!!
    // 데이터를 지우고 다시 새로 만들어주는 역할!
    // 테스트의 독립성을 부여해주는 역할을 한다.
    // 📌 테스트는 순서에 의존하도록 작성하면 안된다!!
    @BeforeEach
    public void beforeEach() {
        truncateArticleTable();
        makeArticleTestDate();
    }

    private void makeArticleTestDate() {
        IntStream.rangeClosed(1, TEST_DATE_SIZE).forEach(no -> {
            boolean isBlind = no >= 11 && no <= 20;
            String title = "제목%d".formatted(no);
            String body = "내용%d".formatted(no);

            myMap.run("""
                    INSERT INTO article
                    SET createdDate = NOW(),
                    modifiedDate = NOW(),
                    title = ?,
                    `body` = ?,
                    isBlind = ?
                    """, title, body, isBlind);
        });
    }

    private void truncateArticleTable() {
        myMap.run("TRUNCATE article");
    }

    @Test
    public void articleService가_존재한다() {
        assertThat(articleService).isNotNull();
    }

    @Test
    @DisplayName("전체 게시물 가져오기!")
    public void getArticles() {
        List<ArticleDto> articleDtoList = articleService.getArticles();
        assertThat(articleDtoList.size()).isEqualTo(TEST_DATE_SIZE);
    }

    @Test
    @DisplayName("id로 게시물 가져오기!")
    public void getArticleById() {
        ArticleDto articleDto = articleService.getArticleById(1);

        assertThat(articleDto.getId()).isEqualTo(1L);
        assertThat(articleDto.getTitle()).isEqualTo("제목1");
        assertThat(articleDto.getBody()).isEqualTo("내용1");
        assertThat(articleDto.getCreatedDate()).isNotNull();
        assertThat(articleDto.getModifiedDate()).isNotNull();
        assertThat(articleDto.isBlind()).isFalse();
    }

    @Test
    @DisplayName("전체 게시물 개수 테스트!")
    public void getArticlesCount() {
        long articlesCount = articleService.getArticlesCount();

        assertThat(articlesCount).isEqualTo(TEST_DATE_SIZE);
    }

    @Test
    @DisplayName("글 작성 테스트!")
    public void write() {
        long newArticleId = articleService.write("제목 new", "내용 new", false);
        ArticleDto articleDto = articleService.getArticleById(newArticleId);

        assertThat(articleDto.getId()).isEqualTo(newArticleId);
        assertThat(articleDto.getTitle()).isEqualTo("제목 new");
        assertThat(articleDto.getBody()).isEqualTo("내용 new");
        assertThat(articleDto.getCreatedDate()).isNotNull();
        assertThat(articleDto.getModifiedDate()).isNotNull();
        assertThat(articleDto.isBlind()).isFalse();
    }

    @Test
    @DisplayName("글 수정 테스트!")
    public void modify() {
        // Ut.sleep(5000);

         articleService.modify(1, "제목 new", "내용 new", true);
        ArticleDto articleDto = articleService.getArticleById(1);

        assertThat(articleDto.getId()).isEqualTo(1);
        assertThat(articleDto.getTitle()).isEqualTo("제목 new");
        assertThat(articleDto.getBody()).isEqualTo("내용 new");
        assertThat(articleDto.isBlind()).isTrue();

        // DB에서 받아온 게시물 수정날짜와 자바에서 계산한 현재 날짜를 비교하여(초단위)
        // 그것이 1초 이하로 차이가 난다면
        // 수정날짜가 갱신되었다 라고 볼 수 있음
        // long diffSeconds = ChronoUnit.SECONDS.between(articleDto.getModifiedDate(), LocalDateTime.now());
        // assertThat(diffSeconds).isLessThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("글 삭제 테스트!")
    public void delete() {
        articleService.delete(1);

        ArticleDto articleDto = articleService.getArticleById(1);

        assertThat(articleDto).isNull();
    }

    @Test
    @DisplayName("이전글 테스트!")
    public void prevArticle() {
        ArticleDto prevArticleDto = articleService.getPrevArticle(2); // 이전글

        assertThat(prevArticleDto.getId()).isEqualTo(1);
        assertThat(prevArticleDto.getTitle()).isEqualTo("제목1");
        assertThat(prevArticleDto.getBody()).isEqualTo("내용1");
    }

    @Test
    @DisplayName("1번 글의 이전글은 존재하지 않는다!")
    public void prevArticleFromFirstArticle() {
        ArticleDto prevArticleDto = articleService.getPrevArticle(1); // 이전글

        assertThat(prevArticleDto).isNull();
    }

    @Test
    @DisplayName("다음글 테스트!")
    public void nextArticle() {
        ArticleDto nextArticleDto = articleService.getNextArticle(2); // 다음글

        assertThat(nextArticleDto.getId()).isEqualTo(3);
        assertThat(nextArticleDto.getTitle()).isEqualTo("제목3");
        assertThat(nextArticleDto.getBody()).isEqualTo("내용3");
    }

    @Test
    @DisplayName("마지막 글의 다음글은 존재하지 않는다!")
    public void nextArticleToLastArticle() {
        ArticleDto nextArticleDto = articleService.getNextArticle(100);

        assertThat(nextArticleDto).isNull();
    }

    @Test
    @DisplayName("10번 글의 다음 글은 21번 글이다! (11번 ~ 20번글 블라인드 처리!)")
    public void nextArticleIsBlind() {
        ArticleDto nextArticleDto = articleService.getNextArticle(10); // 이전글

        assertThat(nextArticleDto.getId()).isEqualTo(21);
    }
}
