import React from 'react';
import styled from 'styled-components';
import {useTheme} from '../hooks/useTheme';
import {ThemeName} from '../types';

const HeaderContainer = styled.header`
    background-color: ${({theme}) => theme.colors.surface};
    padding: 1rem;
    border-bottom: 1px solid ${props => props.theme.colors.border};
`;

const ThemeSelect = styled.select`
    padding: 0.5rem;
    border-radius: ${props => props.theme.sizing.borderRadius.sm};
    border: 1px solid ${props => props.theme.colors.border};
`;

interface HeaderProps {
    onThemeChange: (theme: ThemeName) => void;
}

const Header: React.FC<HeaderProps> = ({onThemeChange}) => {
    const [currentTheme, setTheme] = useTheme();

    return (
        <HeaderContainer>
            <ThemeSelect
                value={currentTheme}
                onChange={(e) => setTheme(e.target.value as any)}
            >
                <option value="main">Day</option>
                <option value="night">Night</option>
                <option value="forest">Forest</option>
                <option value="pony">Bubblegum</option>
                <option value="alien">Alien</option>
            </ThemeSelect>
        </HeaderContainer>
    );
};

export default Header;