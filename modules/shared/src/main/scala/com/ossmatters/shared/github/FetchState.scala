package com.ossmatters.shared.github

enum FetchState:
  def transition(event: FetchEvent): FetchState = this match
    case Repository =>
      event match
        case FetchEvent.RepositoryFetched => PullRequests
        case FetchEvent.RepositoryFetched | FetchEvent.PullRequestsFetched | FetchEvent.IssuesFetched |
            FetchEvent.IssueEventsFetched =>
          this
    case PullRequests =>
      event match
        case FetchEvent.PullRequestsFetched => Issues
        case FetchEvent.RepositoryFetched | FetchEvent.PullRequestsFetched | FetchEvent.IssuesFetched |
            FetchEvent.IssueEventsFetched =>
          this
    case Issues =>
      event match
        case FetchEvent.IssuesFetched => IssueEvents
        case FetchEvent.RepositoryFetched | FetchEvent.PullRequestsFetched | FetchEvent.IssuesFetched |
            FetchEvent.IssueEventsFetched =>
          this
    case IssueEvents =>
      event match
        case FetchEvent.IssueEventsFetched => Fetched
        case FetchEvent.RepositoryFetched | FetchEvent.PullRequestsFetched | FetchEvent.IssuesFetched |
            FetchEvent.IssueEventsFetched =>
          this
    case Fetched => this

  case Repository, PullRequests, Issues, IssueEvents, Fetched

enum FetchEvent:
  case RepositoryFetched, PullRequestsFetched, IssuesFetched, IssueEventsFetched
