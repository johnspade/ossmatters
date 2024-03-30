# OSS Matters

> Track, Reward and Amplify Your Team's Open Source Contributions

Application for tracking employee activity in selected Github repositories.

![Projects page](images/feature-1.png)
![People page](images/feature-2.png)

## Setup

### Environment Variables

- `DATABASE_URL=postgresql://postgres:postgres@localhost:5432/oss`
- `GITHUB_API_TOKEN` - your Github token
- `CLIENT_ID` - Github OAuth App client id
- `CLIENT_SECRET` - Github OAuth App client secret
- `JWT_SECRET` - secret key to generate JWTs
- `GITHUB_CONFIG_PATH=~/.config/ossmatters.conf`
- `AUTH_REDIRECT_URI` - Github authorization callback URL

### Github Config

```
repos = [
	"owner/repo",
    ...
]
usernames = [
    "github_username",
    ...
]
```

## Built with

- zio-http
- HTMX
- Tailwind CSS
- Caliban
