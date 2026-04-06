-- ============================================================
-- RhythmWeaver – Supabase Schema
-- Run this entire file in Supabase → SQL Editor → New Query
-- ============================================================

-- 1. PROFILES (extends auth.users)
create table if not exists public.profiles (
  id          uuid primary key references auth.users(id) on delete cascade,
  username    text unique not null,
  avatar_url  text,
  created_at  timestamptz default now()
);
alter table public.profiles enable row level security;
create policy "Users can view own profile"   on public.profiles for select using (auth.uid() = id);
create policy "Users can update own profile" on public.profiles for update using (auth.uid() = id);
create policy "Users can insert own profile" on public.profiles for insert with check (auth.uid() = id);

-- 2. LIKED SONGS (permanent until user removes)
create table if not exists public.liked_songs (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references auth.users(id) on delete cascade,
  song_id    text not null,
  song_data  jsonb not null,
  liked_at   timestamptz default now(),
  unique(user_id, song_id)
);
alter table public.liked_songs enable row level security;
create policy "Users manage own liked songs" on public.liked_songs
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- 3. RECENTLY PLAYED (auto-deleted after 24 hours)
create table if not exists public.recently_played (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references auth.users(id) on delete cascade,
  song_id    text not null,
  song_data  jsonb not null,
  played_at  timestamptz default now(),
  unique(user_id, song_id)
);
alter table public.recently_played enable row level security;
create policy "Users manage own recents" on public.recently_played
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

create index if not exists recently_played_played_at_idx
  on public.recently_played(played_at);

-- 4. PLAYLISTS
create table if not exists public.playlists (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references auth.users(id) on delete cascade,
  name        text not null,
  cover_art   text,
  created_at  timestamptz default now()
);
alter table public.playlists enable row level security;
create policy "Users manage own playlists" on public.playlists
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- 5. PLAYLIST SONGS
create table if not exists public.playlist_songs (
  id          uuid primary key default gen_random_uuid(),
  playlist_id uuid not null references public.playlists(id) on delete cascade,
  user_id     uuid not null references auth.users(id) on delete cascade,
  song_id     text not null,
  song_data   jsonb not null,
  added_at    timestamptz default now(),
  unique(playlist_id, song_id)
);
alter table public.playlist_songs enable row level security;
create policy "Users manage own playlist songs" on public.playlist_songs
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- 6. AUTO-CLEANUP: delete recents older than 24h
create or replace function public.cleanup_old_recents(p_user_id uuid)
returns void language plpgsql security definer as $$
begin
  delete from public.recently_played
  where user_id = p_user_id
    and played_at < now() - interval '24 hours';
end;
$$;

-- 7. AUTO-CREATE PROFILE on signup
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer as $$
begin
  insert into public.profiles(id, username)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'username', split_part(new.email, '@', 1))
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();