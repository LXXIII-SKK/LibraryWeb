alter table book
    add column has_cover_image boolean not null default false;

create table book_cover (
    book_id bigint primary key references book(id) on delete cascade,
    file_name varchar(255) not null,
    content_type varchar(100) not null,
    content bytea not null,
    updated_at timestamp not null default current_timestamp
);

insert into book_cover (book_id, file_name, content_type, content, updated_at)
values
    (1, 'domain-driven-design.svg', 'image/svg+xml', convert_to($book1$
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 600">
  <rect width="400" height="600" fill="#0f2f44"/>
  <rect x="24" y="24" width="352" height="552" rx="26" fill="#183f59"/>
  <rect x="48" y="52" width="304" height="172" rx="18" fill="#d9a54d"/>
  <text x="60" y="120" fill="#10212f" font-family="Georgia, serif" font-size="34" font-weight="700">Domain-Driven</text>
  <text x="60" y="162" fill="#10212f" font-family="Georgia, serif" font-size="34" font-weight="700">Design</text>
  <text x="60" y="214" fill="#10212f" font-family="Arial, sans-serif" font-size="18">Eric Evans</text>
  <text x="60" y="314" fill="#f7f3e9" font-family="Arial, sans-serif" font-size="18">Architecture shelf</text>
  <text x="60" y="360" fill="#f7f3e9" font-family="Georgia, serif" font-size="22">Strategic design patterns for</text>
  <text x="60" y="392" fill="#f7f3e9" font-family="Georgia, serif" font-size="22">complex domains.</text>
</svg>
$book1$, 'UTF8'), current_timestamp),
    (2, 'designing-data-intensive-applications.svg', 'image/svg+xml', convert_to($book2$
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 600">
  <rect width="400" height="600" fill="#1a2731"/>
  <rect x="26" y="26" width="348" height="548" rx="26" fill="#22414d"/>
  <circle cx="312" cy="114" r="70" fill="#7fd3c5"/>
  <text x="54" y="116" fill="#eff7f4" font-family="Georgia, serif" font-size="27" font-weight="700">Designing Data-</text>
  <text x="54" y="150" fill="#eff7f4" font-family="Georgia, serif" font-size="27" font-weight="700">Intensive</text>
  <text x="54" y="184" fill="#eff7f4" font-family="Georgia, serif" font-size="27" font-weight="700">Applications</text>
  <text x="54" y="228" fill="#b8ece3" font-family="Arial, sans-serif" font-size="18">Martin Kleppmann</text>
  <text x="54" y="336" fill="#d7f8f2" font-family="Georgia, serif" font-size="22">Storage, streams, and</text>
  <text x="54" y="368" fill="#d7f8f2" font-family="Georgia, serif" font-size="22">distributed systems.</text>
</svg>
$book2$, 'UTF8'), current_timestamp),
    (3, 'clean-architecture.svg', 'image/svg+xml', convert_to($book3$
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 600">
  <rect width="400" height="600" fill="#efe5d2"/>
  <rect x="24" y="24" width="352" height="552" rx="28" fill="#f7f0e4"/>
  <path d="M72 120h256v110H72z" fill="#243343"/>
  <text x="88" y="170" fill="#f5f2e8" font-family="Georgia, serif" font-size="38" font-weight="700">Clean</text>
  <text x="88" y="214" fill="#f5f2e8" font-family="Georgia, serif" font-size="38" font-weight="700">Architecture</text>
  <text x="88" y="268" fill="#243343" font-family="Arial, sans-serif" font-size="18">Robert C. Martin</text>
  <text x="88" y="344" fill="#243343" font-family="Georgia, serif" font-size="22">Boundaries, policy, and</text>
  <text x="88" y="376" fill="#243343" font-family="Georgia, serif" font-size="22">long-lived structure.</text>
</svg>
$book3$, 'UTF8'), current_timestamp),
    (4, 'refactoring.svg', 'image/svg+xml', convert_to($book4$
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 600">
  <rect width="400" height="600" fill="#552b2f"/>
  <rect x="28" y="28" width="344" height="544" rx="24" fill="#71363b"/>
  <rect x="54" y="62" width="292" height="86" rx="16" fill="#f0c98c"/>
  <text x="74" y="118" fill="#4f2428" font-family="Georgia, serif" font-size="38" font-weight="700">Refactoring</text>
  <text x="74" y="174" fill="#f4dfbc" font-family="Arial, sans-serif" font-size="18">Martin Fowler</text>
  <text x="74" y="322" fill="#f9edd8" font-family="Georgia, serif" font-size="23">Improving the design of</text>
  <text x="74" y="354" fill="#f9edd8" font-family="Georgia, serif" font-size="23">existing code safely.</text>
</svg>
$book4$, 'UTF8'), current_timestamp),
    (5, 'accelerate.svg', 'image/svg+xml', convert_to($book5$
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 600">
  <rect width="400" height="600" fill="#162b26"/>
  <rect x="30" y="30" width="340" height="540" rx="24" fill="#1e4038"/>
  <path d="M70 136h260l-60 94H130z" fill="#7fd0a4"/>
  <text x="82" y="242" fill="#effaf2" font-family="Georgia, serif" font-size="40" font-weight="700">Accelerate</text>
  <text x="82" y="290" fill="#a7e1be" font-family="Arial, sans-serif" font-size="18">Nicole Forsgren</text>
  <text x="82" y="334" fill="#a7e1be" font-family="Arial, sans-serif" font-size="18">Jez Humble and Gene Kim</text>
  <text x="82" y="404" fill="#effaf2" font-family="Georgia, serif" font-size="22">Delivery performance as an</text>
  <text x="82" y="436" fill="#effaf2" font-family="Georgia, serif" font-size="22">organizational capability.</text>
</svg>
$book5$, 'UTF8'), current_timestamp),
    (6, 'team-topologies.svg', 'image/svg+xml', convert_to($book6$
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 600">
  <rect width="400" height="600" fill="#15304f"/>
  <rect x="28" y="28" width="344" height="544" rx="22" fill="#1e4a71"/>
  <circle cx="116" cy="126" r="34" fill="#f6bb59"/>
  <circle cx="200" cy="126" r="34" fill="#f6bb59"/>
  <circle cx="284" cy="126" r="34" fill="#f6bb59"/>
  <text x="62" y="238" fill="#eef5fb" font-family="Georgia, serif" font-size="34" font-weight="700">Team</text>
  <text x="62" y="278" fill="#eef5fb" font-family="Georgia, serif" font-size="34" font-weight="700">Topologies</text>
  <text x="62" y="326" fill="#c9dff1" font-family="Arial, sans-serif" font-size="18">Matthew Skelton</text>
  <text x="62" y="370" fill="#c9dff1" font-family="Arial, sans-serif" font-size="18">and Manuel Pais</text>
</svg>
$book6$, 'UTF8'), current_timestamp),
    (7, 'building-evolutionary-architectures.svg', 'image/svg+xml', convert_to($book7$
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 600">
  <rect width="400" height="600" fill="#292334"/>
  <rect x="26" y="26" width="348" height="548" rx="28" fill="#3b3150"/>
  <path d="M64 400L176 166l68 132 90-188" fill="none" stroke="#ffb365" stroke-width="18" stroke-linecap="round" stroke-linejoin="round"/>
  <text x="58" y="122" fill="#f7f0ff" font-family="Georgia, serif" font-size="28" font-weight="700">Building</text>
  <text x="58" y="156" fill="#f7f0ff" font-family="Georgia, serif" font-size="28" font-weight="700">Evolutionary</text>
  <text x="58" y="190" fill="#f7f0ff" font-family="Georgia, serif" font-size="28" font-weight="700">Architectures</text>
  <text x="58" y="236" fill="#d9cff1" font-family="Arial, sans-serif" font-size="18">Neal Ford</text>
</svg>
$book7$, 'UTF8'), current_timestamp),
    (8, 'release-it.svg', 'image/svg+xml', convert_to($book8$
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 600">
  <rect width="400" height="600" fill="#2d1f17"/>
  <rect x="24" y="24" width="352" height="552" rx="24" fill="#4a2f1f"/>
  <rect x="58" y="70" width="284" height="74" rx="14" fill="#ff9c4a"/>
  <text x="88" y="120" fill="#402414" font-family="Georgia, serif" font-size="40" font-weight="700">Release It!</text>
  <text x="58" y="220" fill="#ffe6d1" font-family="Arial, sans-serif" font-size="18">Michael T. Nygard</text>
  <text x="58" y="336" fill="#ffe6d1" font-family="Georgia, serif" font-size="23">Production-ready systems,</text>
  <text x="58" y="368" fill="#ffe6d1" font-family="Georgia, serif" font-size="23">stability, and resilience.</text>
</svg>
$book8$, 'UTF8'), current_timestamp);

update book
set has_cover_image = true
where id between 1 and 8;
