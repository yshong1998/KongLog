-- 태그 이름은 대소문자를 구분하지 않고 같은지 비교한다. spring, Spring, SPRING은 같은 태그다.
-- 표기는 처음 만든 그대로 저장한다. 그래서 칸의 값은 바꾸지 않고, 비교할 때만 lower()를 쓴다.
--
-- 대소문자까지 같은 이름만 막던 규칙을 지우고, 소문자로 바꾼 이름이 같은 것을 막는 색인으로 바꾼다.
-- 새 색인은 옛 규칙이 막던 것도 모두 막는다.
-- 글 목록의 태그 필터도 lower(name)으로 찾는다. 이 색인이 그 조회에도 쓰인다.
--
-- 소문자로 바꾸는 일은 자바가 아니라 DB의 lower() 한 곳에서만 한다.
-- 자바의 toLowerCase()와 DB의 lower()는 몇몇 글자에서 결과가 다를 수 있다.
--
-- 이미 대소문자만 다른 태그가 둘 이상 있으면 색인을 만들지 못해 이 파일이 실패한다.

alter table tag drop constraint uq_tag_name;

create unique index uq_tag_name_lower on tag (lower(name));
