import math
import numpy as np
import matplotlib.pyplot as plt
import sys


def normal_distribution(u, sig):
    x = np.linspace(u-3*sig, u+3*sig, 100)
    y = np.exp(-(x-u)**2/(2*sig**2)) / (math.sqrt(2*math.pi)*sig)
    return (x, y)


def draw_single_normal_distribution(title, loc, u, sig_square):
    sig = math.sqrt(sig_square)
    x, y = normal_distribution(u, sig)
    plt.subplot(loc)
    plt.xlabel("pixel", fontsize=10)
    plt.ylabel("probablity", fontsize=10)
    plt.title(title, fontsize=16)
    plt.plot(x, y, color="black", linewidth=2)


def draw_normal_distribution(title, loc, u, sig_square, color, label):
    sig = math.sqrt(sig_square)
    x, y = normal_distribution(u, sig)
    plt.subplot(loc)
    plt.xlabel("pixel", fontsize=10)
    plt.ylabel("probablity", fontsize=10)
    plt.title(title, fontsize=16)
    plt.plot(x, y, linewidth=2, color=color, label=label)


def draw_duplicate_normal_distribution(title, loc, u_list, sig_square_list, label_list, color_list):
    n = len(u_list)
    for i in range(0, n):
        draw_normal_distribution(
            title, loc, u_list[i], sig_square_list[i], color_list[i], label_list[i])
    plt.legend()


def run():
    u_list = [158, 122, 216]
    sig_square_list = [1186, 1677, 5273]
    label_list = ["plain", "normal", "complicated"]
    color_list = ["red", "blue", "yellow"]

    plt.figure(figsize=(12, 8))
    u = u_list[0]
    sig_square = sig_square_list[0]
    suffix = "($\mu$={}, $\sigma$={})".format(u, math.sqrt(sig_square))
    draw_single_normal_distribution(
        title="plain"+suffix, loc=231, u=u, sig_square=sig_square)
    u = u_list[1]
    sig_square = sig_square_list[1]
    suffix = "($\mu$={}, $\sigma$={})".format(u, math.sqrt(sig_square))
    draw_single_normal_distribution(
        title="normal"+suffix, loc=232, u=u, sig_square=sig_square)
    u = u_list[2]
    sig_square = sig_square_list[2]
    suffix = "($\mu$={}, $\sigma$={})".format(u, math.sqrt(sig_square))
    draw_single_normal_distribution(
        title="complicated"+suffix, loc=233, u=u, sig_square=sig_square)
    draw_duplicate_normal_distribution(
        title="overall", loc=212, u_list=u_list, sig_square_list=sig_square_list, label_list=label_list, color_list=color_list)

    plt.show()


if __name__ == "__main__":
    run()
