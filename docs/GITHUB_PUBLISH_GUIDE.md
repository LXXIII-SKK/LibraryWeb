# GitHub Publish Guide

This project is safe to publish as a demo repository after a normal git review. The credentials in the repo are local development/demo credentials, not production secrets.

## 1. Check the repo state first

From the project root:

```bash
git status
```

At the moment this repository has a mixed state:

- some files are already staged
- many newer files are still untracked
- there is no Git remote configured yet

## 2. Normalize the index before committing

If you want one clean initial commit, unstage the old partial snapshot first:

```bash
git reset
```

This does not delete your work. It only moves files out of the staging area.

Then stage the full project:

```bash
git add .
```

Check again:

```bash
git status
```

## 3. Review what will be published

Before committing, verify these areas:

- `.gitignore` excludes `target/`, `frontend/node_modules/`, `frontend/dist/`, logs, and local env files
- top-level docs are included: `README.md`, `PROJECT_DOCUMENTATION.md`, `SETUP_GUIDE.md`, `HELP.md`
- demo credentials are clearly described as local/demo-only
- no personal machine files are staged from `.idea/` or local caches

Useful checks:

```bash
git diff --cached --stat
git diff --cached
```

## 4. Create the initial commit

```bash
git commit -m "Initial publish of mini library system"
```

## 5. Rename the branch to `main`

GitHub defaults to `main` for new repositories, so rename your local branch before pushing:

```bash
git branch -M main
```

## 6. Create the GitHub repository

On GitHub:

1. click `New repository`
2. choose a repository name, for example `mini-library`
3. keep it empty
4. do not add a README, `.gitignore`, or license from GitHub if you already want to push this working tree as-is

## 7. Connect the remote

Replace `<your-user>` and `<repo>`:

```bash
git remote add origin https://github.com/<your-user>/<repo>.git
```

Verify:

```bash
git remote -v
```

## 8. Push the project

```bash
git push -u origin main
```

## 9. Recommended follow-up

After the first push, add these if you want the repo to look more complete:

- a `LICENSE`
- repository description and topics on GitHub
- screenshots in the README
- GitHub Actions for backend tests and frontend build

## 10. If GitHub rejects the push

Common causes:

- large generated files accidentally staged
- remote already contains commits
- authentication is not configured yet

Useful fixes:

```bash
git status
git remote -v
git fetch origin
```

If the remote was created with extra commits, stop and inspect before forcing anything.
