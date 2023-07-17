create table github_repositories(
    id varchar(255) not null 
        constraint github_repositories_pkey primary key,  
    owner varchar(255) not null,
    name varchar(255) not null,
    fetch_state varchar(255) not null,
    sync_cutoff timestamp not null
);

create table github_issues(
    id varchar(255) not null 
        constraint github_issues_pkey primary key,
    type varchar(255) not null,
    repository_id varchar(255) not null
        constraint github_issues_repository_id_fkey references github_repositories,    
    title text not null,
    url text not null,
    created_at timestamp not null,
    author varchar(255),
    has_more_events boolean not null
);

create table github_issue_events(
    id varchar(255) not null 
        constraint github_issue_events_pkey primary key,
    type varchar(255) not null,
    issue_id varchar(255) not null
        constraint github_issue_events_issue_id_fkey references github_issues,
    created_at timestamp not null,
    author varchar(255)
);
