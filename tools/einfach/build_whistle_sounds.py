"""Synthesize original mono whistle calls; requires numpy and soundfile (libsndfile/Vorbis)."""
from pathlib import Path
import soundfile as sf
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'src/main/resources/assets/deathsareitems/sounds'
RATE = 44100


def call(name, notes):
    rng = np.random.default_rng(42)
    pieces = []
    for start, end, duration in notes:
        n = int(RATE * duration)
        t = np.arange(n) / RATE
        frequency = np.linspace(start, end, n) + 9 * np.sin(2 * np.pi * 24 * t)
        phase = np.cumsum(frequency) * (2 * np.pi / RATE)
        # Breath attack/release, a soft octave harmonic and a little air noise.
        envelope = np.minimum(t / .025, 1) * np.minimum((duration - t) / .055, 1)
        air = rng.normal(0, .012, n)
        tone = (.57 * np.sin(phase) + .055 * np.sin(2 * phase) + air) * envelope
        pieces.extend((tone, np.zeros(int(RATE * .075))))
    audio = np.concatenate(pieces) * .79
    OUT.mkdir(parents=True, exist_ok=True)
    sf.write(OUT / f'{name}.ogg', audio, RATE, format='OGG', subtype='VORBIS')


if __name__ == '__main__':
    call('juky_summon', [(1650, 2150, .26), (2150, 2320, .23)])
    call('juky_recall', [(2220, 2050, .18), (2000, 1450, .38)])
