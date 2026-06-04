package com.example.aplicacionchancha.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.aplicacionchancha.fragments.BalancesFragment;
import com.example.aplicacionchancha.fragments.ColectasFragment;
import com.example.aplicacionchancha.fragments.GastosFragment;
import com.example.aplicacionchancha.fragments.MiembrosFragment;

public class GroupPagerAdapter extends FragmentStateAdapter {

    private final int     grupoId;
    private final String  codigoInvitacion;
    private final boolean esAdmin;

    public GroupPagerAdapter(@NonNull FragmentActivity fa, int grupoId,
                             String codigoInvitacion, boolean esAdmin) {
        super(fa);
        this.grupoId          = grupoId;
        this.codigoInvitacion = codigoInvitacion;
        this.esAdmin          = esAdmin;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return GastosFragment.newInstance(grupoId, esAdmin);
            case 1: return BalancesFragment.newInstance(grupoId);
            case 2: return MiembrosFragment.newInstance(grupoId, codigoInvitacion);
            default: return ColectasFragment.newInstance(grupoId, esAdmin);
        }
    }

    @Override
    public int getItemCount() { return 4; }
}
